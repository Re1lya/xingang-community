package com.xingang.community.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.common.exception.BusinessException;
import com.xingang.community.common.util.SnowflakeIdWorker;
import com.xingang.community.common.util.UserContext;
import com.xingang.community.entity.SeckillVoucher;
import com.xingang.community.entity.VoucherOrder;
import com.xingang.community.order.mapper.VoucherOrderMapper;
import com.xingang.community.order.service.VoucherOrderService;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单服务。
 *
 * <h3>秒杀链路</h3>
 * <pre>
 *   seckillVoucher (入口线程)
 *     → 校验登录态、秒杀时间
 *     → 生成订单ID
 *     → 执行 lua/seckill.lua (原子操作)
 *     → 返回订单ID 或 错误
 *
 *   processStreamOrders (后台消费者)
 *     → XREADGROUP 阻塞读取 stream.orders
 *     → Redisson 用户锁
 *     → 事务创建订单
 *     → ACK 消息
 * </pre>
 *
 * <h3>Lua脚本返回值</h3>
 * <pre>
 *   0 = 成功入队
 *   1 = 库存不足
 *   2 = 重复下单
 * </pre>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl implements VoucherOrderService {

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private DefaultRedisScript<Long> seckillScript;

    private static final String STREAM_CONSUMER_GROUP = "order-group";
    private static final String STREAM_CONSUMER_NAME = "order-consumer-1";

    @PostConstruct
    public void init() {
        // 加载Lua脚本
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);

        // 初始化消费组（服务启动时执行一次）
        initStreamConsumerGroup();
    }

    // ==================== 秒杀入口 ====================

    @Override
    public Long seckillVoucher(Long voucherId) {
        // 1. 校验登录态
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // 2. 校验秒杀券存在及时间窗口
        SeckillVoucher sv = seckillVoucherMapper.selectById(voucherId);
        if (sv == null) {
            throw new BusinessException(ErrorCode.SECKILL_VOUCHER_NOT_FOUND, "秒杀券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sv.getBeginTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED, "秒杀尚未开始");
        }
        if (now.isAfter(sv.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED, "秒杀已结束");
        }

        // 3. 生成全局订单ID（雪花算法），不落库，只传入Lua脚本
        // 订单最终由后台Stream消费者异步创建
        Long orderId = SnowflakeIdWorker.nextId();

        // 4. 执行Lua脚本
        // KEYS[1] = seckill:stock:{voucherId}
        // KEYS[2] = seckill:order:{voucherId}（已抢券用户集合）
        // KEYS[3] = stream.orders
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderSetKey = RedisConstants.SECKILL_ORDER_KEY + voucherId;
        String streamKey = RedisConstants.STREAM_ORDERS_KEY;

        Long result = stringRedisTemplate.execute(
                seckillScript,
                List.of(stockKey, orderSetKey, streamKey),
                String.valueOf(orderId),
                String.valueOf(userId),
                String.valueOf(voucherId)
        );

        // 5. 处理返回值
        if (result == null) {
            log.error("Seckill lua returned null: voucherId={}, userId={}", voucherId, userId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "秒杀异常，请稍后重试");
        }

        int r = result.intValue();
        switch (r) {
            case 0:
                log.info("Seckill success (queued): orderId={}, voucherId={}, userId={}",
                        orderId, voucherId, userId);
                return orderId;
            case 1:
                throw new BusinessException(ErrorCode.SECKILL_STOCK_NOT_ENOUGH, "优惠券库存不足");
            case 2:
                throw new BusinessException(ErrorCode.SECKILL_DUPLICATE_ORDER, "您已抢过该优惠券");
            default:
                log.error("Seckill lua unknown result: {}", r);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "秒杀异常，请稍后重试");
        }
    }

    // ==================== 后台Stream消费者（骨架预留） ====================

    /**
     * 初始化Redis Stream消费组。
     * 在 {@link #init()} 中调用。
     */
    private void initStreamConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(
                    RedisConstants.STREAM_ORDERS_KEY,
                    ReadOffset.from("0"),
                    STREAM_CONSUMER_GROUP
            );
            log.info("Stream consumer group created: {} → {}", RedisConstants.STREAM_ORDERS_KEY, STREAM_CONSUMER_GROUP);
        } catch (Exception e) {
            // 消费组已存在时会抛异常，忽略
            log.info("Stream consumer group may already exist: {}", e.getMessage());
        }
    }

    /**
     * 后台消费者主循环（骨架预留）。
     *
     * <p>完整实现应在新线程中启动此方法，典型流程：</p>
     * <pre>
     * 1. XREADGROUP BLOCK 阻塞读取消息
     * 2. 解析消息中的 orderId、userId、voucherId
     * 3. 加 Redisson 用户锁 lock:order:{userId}
     * 4. 调用 createOrderInTransaction 事务创建订单
     * 5. XACK 确认消息
     * 6. 异常时进入 pending-list 处理流程
     * </pre>
     *
     * <p>启用方式：在Spring托管后启动单独线程</p>
     * <pre>
     *   &#64;PostConstruct
     *   public void startConsumer() {
     *       new Thread(this::processStreamOrders, "seckill-order-consumer").start();
     *   }
     * </pre>
     */
    @SuppressWarnings("unused")
    private void processStreamOrders() {
        while (true) {
            try {
                // 1. 阻塞读取Stream消息
                List<MapRecord<String, Object, Object>> messages = stringRedisTemplate.opsForStream()
                        .read(
                                Consumer.from(STREAM_CONSUMER_GROUP, STREAM_CONSUMER_NAME),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.lastConsumed())
                        );

                if (messages == null || messages.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : messages) {
                    Map<Object, Object> body = record.getValue();
                    // 2. 解析消息
                    String orderId = String.valueOf(body.get("orderId"));
                    String userId = String.valueOf(body.get("userId"));
                    String voucherId = String.valueOf(body.get("voucherId"));

                    // 3. 加Redisson用户锁
                    String lockKey = RedisConstants.LOCK_ORDER_KEY + userId;
                    RLock lock = redissonClient.getLock(lockKey);
                    boolean locked = lock.tryLock(RedisConstants.LOCK_ORDER_TTL_SECONDS, TimeUnit.SECONDS);

                    if (!locked) {
                        log.warn("Failed to acquire lock: key={}", lockKey);
                        continue; // 不ACK，后续pending-list处理
                    }

                    try {
                        // 4. 事务创建订单
                        createOrderInTransaction(Long.valueOf(orderId), Long.valueOf(userId), Long.valueOf(voucherId));

                        // 5. ACK消息
                        stringRedisTemplate.opsForStream().acknowledge(
                                RedisConstants.STREAM_ORDERS_KEY,
                                STREAM_CONSUMER_GROUP,
                                record.getId().getValue()
                        );
                        log.info("Order created and acked: orderId={}", orderId);
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Stream consumer error", e);
                // 异常不退出循环，pending-list由后续处理
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 处理pending-list中的消息（骨架预留）。
     *
     * <p>消费组中存在pending-list时，应调用此方法逐条重新处理或转移。</p>
     * <pre>
     * 1. XPENDING 查询待处理消息
     * 2. XCLAIM 认领或移交
     * 3. 重新执行创建订单逻辑
     * 4. XACK
     * </pre>
     */
    @SuppressWarnings("unused")
    private void handlePendingList() {
        // 骨架预留，后续实现
        PendingMessagesSummary pending = stringRedisTemplate.opsForStream()
                .pending(RedisConstants.STREAM_ORDERS_KEY, STREAM_CONSUMER_GROUP);
        log.info("Pending messages count: {}", pending.getTotalPendingMessages());
        // TODO: 遍历pending → XCLAIM → 重试 → ACK
    }

    // ==================== 事务创建订单 ====================

    /**
     * 在事务中二次校验并创建订单。
     *
     * <p>数据库扣减库存使用乐观条件 stock > 0，防止最终库存超卖。</p>
     *
     * @param orderId   秒杀入口生成的订单ID
     * @param userId    用户ID
     * @param voucherId 秒杀券ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrderInTransaction(Long orderId, Long userId, Long voucherId) {
        // 1. 检查订单是否已存在（二次校验一人一单）
        Long existCount = voucherOrderMapper.selectCount(
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getUserId, userId)
                        .eq(VoucherOrder::getVoucherId, voucherId)
        );
        if (existCount > 0) {
            log.warn("Duplicate order prevented in transaction: userId={}, voucherId={}", userId, voucherId);
            return;
        }

        // 2. 乐观扣减MySQL库存 (WHERE stock > 0)
        int rows = seckillVoucherMapper.deductStock(voucherId);
        if (rows == 0) {
            log.warn("DB stock deduct failed (stock=0): voucherId={}", voucherId);
            throw new BusinessException(ErrorCode.SECKILL_STOCK_NOT_ENOUGH, "库存不足");
        }

        // 3. 插入订单记录
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(1); // 已创建
        voucherOrderMapper.insert(order);

        log.info("Order created in DB: orderId={}, userId={}, voucherId={}", orderId, userId, voucherId);
    }
}
