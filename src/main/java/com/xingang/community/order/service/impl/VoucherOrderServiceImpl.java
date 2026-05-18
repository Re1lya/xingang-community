package com.xingang.community.order.service.impl;

import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.common.exception.BusinessException;
import com.xingang.community.common.util.SnowflakeIdWorker;
import com.xingang.community.common.util.UserContext;
import com.xingang.community.entity.SeckillVoucher;
import com.xingang.community.order.service.VoucherOrderService;
import com.xingang.community.order.service.VoucherOrderTransactionService;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
 *   processStreamOrders (后台消费者，Spring托管线程池)
 *     → XREADGROUP 阻塞读取 stream.orders
 *     → Redisson 用户锁
 *     → 事务创建订单
 *     → ACK 消息
 *
 *   handlePendingList (异常恢复)
 *     → XPENDING 查询待处理消息
 *     → XREADGROUP 读取 + Redisson锁 + 事务重试
 *     → ACK
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
    private VoucherOrderTransactionService voucherOrderTransactionService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    @Qualifier("streamConsumerExecutor")
    private ThreadPoolTaskExecutor streamConsumerExecutor;

    private DefaultRedisScript<Long> seckillScript;

    private static final String STREAM_CONSUMER_GROUP = "order-group";
    private static final String STREAM_CONSUMER_NAME = "order-consumer-1";

    private static final int MAX_DELIVERY_COUNT = 3;

    private volatile boolean consumerRunning = true;

    @PostConstruct
    public void init() {
        // 加载Lua脚本
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);

        // 初始化消费组（服务启动时执行一次）
        initStreamConsumerGroup();

        // 启动Spring管理的后台消费者
        streamConsumerExecutor.submit(this::processStreamOrders);
        log.info("Stream consumer submitted to managed executor");
    }

    @PreDestroy
    public void shutdown() {
        consumerRunning = false;
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
        Long orderId = SnowflakeIdWorker.nextId();

        // 4. 执行Lua脚本（原子操作：库存校验 + 一人一单校验 + 扣库存 + 入队）
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

    // ==================== Stream 消费组初始化 ====================

    /**
     * 初始化Redis Stream消费组，使用 XGROUP CREATE ... MKSTREAM。
     *
     * <p>MKSTREAM 确保空 Stream 情况下也能创建消费组，
     * 不会写入业务订单假消息。</p>
     *
     * <p>消费组已存在（BUSYGROUP）时可安全忽略；
     * 其他异常（网络、权限等）必须抛出 {@link IllegalStateException}，避免服务假启动。</p>
     */
    private void initStreamConsumerGroup() {
        String streamKey = RedisConstants.STREAM_ORDERS_KEY;
        try {
            stringRedisTemplate.execute((RedisCallback<String>) connection ->
                    connection.streamCommands().xGroupCreate(
                            streamKey.getBytes(StandardCharsets.UTF_8),
                            STREAM_CONSUMER_GROUP,
                            ReadOffset.from("0"),
                            true  // MKSTREAM：Stream 不存在时自动创建
                    )
            );
            log.info("Stream consumer group created: {} → {}", streamKey, STREAM_CONSUMER_GROUP);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("BUSYGROUP")) {
                log.info("Stream consumer group already exists: {} → {}", streamKey, STREAM_CONSUMER_GROUP);
            } else {
                log.error("Failed to create stream consumer group: {} → {}", streamKey, STREAM_CONSUMER_GROUP, e);
                throw new IllegalStateException(
                        "Failed to initialize Redis Stream consumer group: " + streamKey + " → " + STREAM_CONSUMER_GROUP, e);
            }
        }
    }

    // ==================== 后台Stream消费者（Spring托管线程池） ====================

    /**
     * 后台消费者主循环，由Spring管理的ThreadPoolTaskExecutor执行。
     * <ol>
     *   <li>XREADGROUP BLOCK 阻塞读取消息</li>
     *   <li>解析消息中的 orderId、userId、voucherId</li>
     *   <li>加 Redisson 用户锁 lock:order:{userId}</li>
     *   <li>调用 createOrderInTransaction 事务创建订单</li>
     *   <li>XACK 确认消息</li>
     *   <li>异常时保留在pending-list，等待后续恢复处理</li>
     * </ol>
     */
    private void processStreamOrders() {
        while (consumerRunning) {
            try {
                // 1. 阻塞读取Stream消息
                List<MapRecord<String, Object, Object>> messages = stringRedisTemplate.opsForStream()
                        .read(
                                Consumer.from(STREAM_CONSUMER_GROUP, STREAM_CONSUMER_NAME),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.lastConsumed())
                        );

                if (messages == null || messages.isEmpty()) {
                    // 空闲时检查是否有pending消息待处理
                    tryHandlePending();
                    continue;
                }

                for (MapRecord<String, Object, Object> record : messages) {
                    processSingleMessage(record);
                }
            } catch (Exception e) {
                log.error("Stream consumer error", e);
                sleepUninterruptedly(1000);
            }
        }
        log.info("Stream consumer loop exited");
    }

    /**
     * 处理单条Stream消息。
     *
     * @return true 表示事务创建成功且 ACK 成功（ACK 返回值 > 0）；
     *         false 表示获取锁失败、事务失败或 ACK 失败，消息仍保留在 pending-list
     */
    private boolean processSingleMessage(MapRecord<String, Object, Object> record) {
        Map<Object, Object> body = record.getValue();
        String orderId = String.valueOf(body.get("orderId"));
        String userId = String.valueOf(body.get("userId"));
        String voucherId = String.valueOf(body.get("voucherId"));

        String lockKey = RedisConstants.LOCK_ORDER_KEY + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(RedisConstants.LOCK_ORDER_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock interrupted: key={}", lockKey);
            return false;
        }

        if (!locked) {
            log.warn("Failed to acquire lock, pending-list will retry: key={}", lockKey);
            return false;
        }

        try {
            voucherOrderTransactionService.createOrderInTransaction(
                    Long.valueOf(orderId), Long.valueOf(userId), Long.valueOf(voucherId));

            Long ack = stringRedisTemplate.opsForStream().acknowledge(
                    RedisConstants.STREAM_ORDERS_KEY,
                    STREAM_CONSUMER_GROUP,
                    record.getId().getValue()
            );
            if (ack != null && ack > 0) {
                log.info("Order created and acked: orderId={}", orderId);
                return true;
            } else {
                log.error("ACK returned {} for orderId={}, message stays in pending-list", ack, orderId);
                return false;
            }
        } catch (Exception e) {
            log.error("Order creation failed, message stays in pending-list: orderId={}", orderId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== Pending-list 处理 ====================

    /**
     * 基础pending-list处理：先路由超限消息到死信队列，再对剩余消息尝试订单创建。
     *
     * <p>在消费者空闲时自动触发，也可通过外部调用手动触发。</p>
     */
    private void tryHandlePending() {
        try {
            PendingMessagesSummary pending = stringRedisTemplate.opsForStream()
                    .pending(RedisConstants.STREAM_ORDERS_KEY, STREAM_CONSUMER_GROUP);
            long total = pending.getTotalPendingMessages();
            if (total == 0) {
                return;
            }

            log.info("Found {} pending messages, attempting recovery", total);

            // 1. 先将超过最大投递次数的消息路由到死信队列
            int routedToDlq = routeExpiredToDeadLetter();

            // 2. 读取剩余pending消息并重试
            List<MapRecord<String, Object, Object>> pendingRecords = stringRedisTemplate.opsForStream()
                    .read(
                            Consumer.from(STREAM_CONSUMER_GROUP, STREAM_CONSUMER_NAME),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.from("0"))
                    );

            if (pendingRecords == null || pendingRecords.isEmpty()) {
                return;
            }

            int recovered = 0;
            for (MapRecord<String, Object, Object> record : pendingRecords) {
                if (processSingleMessage(record)) {
                    recovered++;
                }
            }
            if (recovered > 0 || routedToDlq > 0) {
                log.info("Pending recovery: {}/{} acked, {} routed to DLQ",
                        recovered, pendingRecords.size(), routedToDlq);
            }
        } catch (Exception e) {
            log.error("Pending-list processing error", e);
        }
    }

    /**
     * 手动触发pending-list处理（供外部调用，如管理接口或定时任务）。
     *
     * @return 成功处理并 ACK 的消息数（只有 ACK 返回值 > 0 才计入）
     */
    @Override
    public int handlePendingList() {
        PendingMessagesSummary pending = stringRedisTemplate.opsForStream()
                .pending(RedisConstants.STREAM_ORDERS_KEY, STREAM_CONSUMER_GROUP);
        long total = pending.getTotalPendingMessages();
        log.info("Manual pending-list processing: {} pending messages", total);

        if (total == 0) {
            return 0;
        }

        // 先路由超限消息到死信队列
        int routedToDlq = routeExpiredToDeadLetter();
        int processed = 0;
        try {
            List<MapRecord<String, Object, Object>> pendingRecords = stringRedisTemplate.opsForStream()
                    .read(
                            Consumer.from(STREAM_CONSUMER_GROUP, STREAM_CONSUMER_NAME),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(RedisConstants.STREAM_ORDERS_KEY, ReadOffset.from("0"))
                    );

            if (pendingRecords != null) {
                for (MapRecord<String, Object, Object> record : pendingRecords) {
                    if (processSingleMessage(record)) {
                        processed++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Manual pending-list processing error", e);
        }
        log.info("Manual pending result: {} acked, {} routed to DLQ", processed, routedToDlq);
        return processed;
    }

    // ==================== 死信队列路由 ====================

    /**
     * 扫描pending-list，将投递次数达到 {@link #MAX_DELIVERY_COUNT} 的消息
     * 写入死信队列 {@code stream.orders.dlq} 并 ACK 原消息。
     *
     * <h3>防误丢设计</h3>
     * <ol>
     *   <li>仅检查 deliveryCount >= MAX_DELIVERY_COUNT，正常消息不受影响</li>
     *   <li>先 XADD 到 DLQ Stream，成功后才 XACK 原消息</li>
     *   <li>XACK 返回值必须 ack != null && ack > 0 才计入 routed 成功数</li>
     *   <li>XADD 成功但 XACK 返回 0/null 时：DLQ 已有副本，原消息留在 pending-list，记录 ERROR</li>
     *   <li>DLQ 保留完整原始字段（orderId/userId/voucherId）+ 失败元信息</li>
     *   <li>XADD 失败时消息留在 pending-list，记录 ERROR 日志</li>
     * </ol>
     *
     * <h3>幂等说明（后续增强）</h3>
     * <p>XADD 成功 + XACK 失败时，下一轮 pending 扫描会再次命中该消息并重复写入 DLQ。
     * 当前设计接受"重复 DLQ 记录"（宁可多一条，不丢一条），运维可通过 originalMessageId 去重。
     * 后续增强方案：新增 Redis Set {@code seckill:dlq:routed}，XADD 前 SISMEMBER 检查
     * originalMessageId，已存在则跳过 XADD、直接重试 XACK。</p>
     *
     * @return 本次成功路由到死信队列的消息数（仅 XADD 成功且 XACK > 0 才计数）
     */
    private int routeExpiredToDeadLetter() {
        int routed = 0;
        try {
            PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
                    .pending(RedisConstants.STREAM_ORDERS_KEY, STREAM_CONSUMER_GROUP,
                            Range.open("-", "+"), 50);

            for (PendingMessage pm : pendingMessages) {
                if (pm.getTotalDeliveryCount() < MAX_DELIVERY_COUNT) {
                    continue;
                }

                // 通过 XRANGE 只读读取消息体（不影响 pending 状态）
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                        .range(RedisConstants.STREAM_ORDERS_KEY,
                                Range.closed(pm.getId().getValue(), pm.getId().getValue()));

                if (records == null || records.isEmpty()) {
                    log.warn("DLQ route: body not found for messageId={}, skip", pm.getId().getValue());
                    continue;
                }

                Map<Object, Object> body = records.get(0).getValue();

                // 构建DLQ消息体：原始字段 + 失败元信息
                Map<String, String> dlqBody = new LinkedHashMap<>();
                dlqBody.put("orderId", String.valueOf(body.getOrDefault("orderId", "")));
                dlqBody.put("userId", String.valueOf(body.getOrDefault("userId", "")));
                dlqBody.put("voucherId", String.valueOf(body.getOrDefault("voucherId", "")));
                dlqBody.put("originalMessageId", pm.getId().getValue());
                dlqBody.put("failureReason", "MAX_DELIVERY_EXCEEDED");
                dlqBody.put("deliveryCount", String.valueOf(pm.getTotalDeliveryCount()));
                dlqBody.put("movedAt", LocalDateTime.now().toString());

                // XADD → 校验 → XACK（只有 ack > 0 才算路由成功）
                try {
                    RecordId dlqId = stringRedisTemplate.opsForStream()
                            .add(RedisConstants.STREAM_ORDERS_DLQ_KEY, dlqBody);

                    if (dlqId == null) {
                        log.error("DLQ XADD returned null for messageId={}, message kept in pending",
                                pm.getId().getValue());
                        continue;
                    }

                    Long ack = stringRedisTemplate.opsForStream()
                            .acknowledge(RedisConstants.STREAM_ORDERS_KEY,
                                    STREAM_CONSUMER_GROUP, pm.getId().getValue());
                    if (ack != null && ack > 0) {
                        routed++;
                        log.warn("DLQ routed and acked: originalId={}, dlqId={}, deliveryCount={}",
                                pm.getId().getValue(), dlqId.getValue(), pm.getTotalDeliveryCount());
                    } else {
                        log.error("DLQ XACK failed after XADD: originalId={}, dlqId={}, ack={}. "
                                        + "DLQ copy exists but message stays in pending-list. "
                                        + "Next pending cycle may create duplicate DLQ entry.",
                                pm.getId().getValue(), dlqId.getValue(), ack);
                    }
                } catch (Exception e) {
                    log.error("DLQ route failed for messageId={}, message kept in pending",
                            pm.getId().getValue(), e);
                }
            }
        } catch (Exception e) {
            log.error("Dead-letter routing scan error", e);
        }
        return routed;
    }

    // ==================== 辅助方法 ====================

    private void sleepUninterruptedly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
