package com.xingang.community.order.service.impl;

import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.entity.SeckillVoucher;
import com.xingang.community.order.dto.PendingListInfo;
import com.xingang.community.order.dto.PendingMessageDetail;
import com.xingang.community.order.dto.SeckillStockVO;
import com.xingang.community.order.service.SeckillObservabilityService;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 秒杀可观测服务实现。
 *
 * <ul>
 *   <li>pending-list 查询：XPENDING 汇总 + XPENDING 详情（含 deliveryCount）+ XRANGE 读 body（只读，不改变消息状态）</li>
 *   <li>库存查询：Redis GET + MySQL SELECT，纯对照，不写入</li>
 * </ul>
 */
@Slf4j
@Service
public class SeckillObservabilityServiceImpl implements SeckillObservabilityService {

    private static final String STREAM_CONSUMER_GROUP = "order-group";
    private static final int MAX_PENDING_DETAILS = 50;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Override
    public PendingListInfo getPendingListInfo() {
        String streamKey = RedisConstants.STREAM_ORDERS_KEY;
        PendingListInfo info = new PendingListInfo();
        info.setStreamKey(streamKey);
        info.setConsumerGroup(STREAM_CONSUMER_GROUP);

        try {
            // XPENDING 汇总
            PendingMessagesSummary summary = stringRedisTemplate.opsForStream()
                    .pending(streamKey, STREAM_CONSUMER_GROUP);
            info.setTotalPending(summary.getTotalPendingMessages());
            if (summary.getTotalPendingMessages() == 0) {
                info.setMessages(Collections.emptyList());
                return info;
            }

            // XPENDING 详情：获取每条消息的 deliveryCount 等元信息
            List<PendingMessageDetail> details = queryPendingDetails(streamKey);
            info.setMessages(details);

            // 从详情列表中计算消息ID范围
            if (!details.isEmpty()) {
                info.setLowId(details.get(0).getMessageId());
                info.setHighId(details.get(details.size() - 1).getMessageId());
            }
        } catch (Exception e) {
            log.error("Failed to query pending-list info", e);
            info.setMessages(Collections.emptyList());
        }
        return info;
    }

    @Override
    public SeckillStockVO getSeckillStock(Long voucherId) {
        SeckillStockVO vo = new SeckillStockVO();
        vo.setVoucherId(voucherId);

        // Redis 库存
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String redisVal = stringRedisTemplate.opsForValue().get(stockKey);
        if (redisVal != null) {
            vo.setRedisStock(Integer.valueOf(redisVal));
        }

        // 数据库库存
        SeckillVoucher sv = seckillVoucherMapper.selectById(voucherId);
        if (sv != null) {
            vo.setDbStock(sv.getStock());
        }
        return vo;
    }

    @Override
    public List<SeckillStockVO> getAllSeckillStocks() {
        List<SeckillStockVO> results = new ArrayList<>();
        List<SeckillVoucher> vouchers = seckillVoucherMapper.selectList(null);
        for (SeckillVoucher sv : vouchers) {
            results.add(getSeckillStock(sv.getVoucherId()));
        }
        return results;
    }

    // ==================== pending详情查询 ====================

    /**
     * 通过 XPENDING range 获取每条pending消息的元信息，再通过 XRANGE 只读读取消息体。
     * XRANGE 不涉及消费组，不会改变 pending 状态。
     */
    private List<PendingMessageDetail> queryPendingDetails(String streamKey) {
        List<PendingMessageDetail> details = new ArrayList<>();

        try {
            // XPENDING stream group - + count — 获取单条消息的 deliveryCount
            PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
                    .pending(streamKey, STREAM_CONSUMER_GROUP,
                            Range.open("-", "+"), MAX_PENDING_DETAILS);

            for (PendingMessage pm : pendingMessages) {
                PendingMessageDetail detail = new PendingMessageDetail();
                detail.setMessageId(pm.getId().getValue());
                detail.setConsumer(pm.getConsumerName());
                Duration elapsed = pm.getElapsedTimeSinceLastDelivery();
                detail.setIdleTimeMs(elapsed != null ? elapsed.toMillis() : null);
                detail.setDeliveryCount(pm.getTotalDeliveryCount());

                // XRANGE messageId messageId — 只读读取消息体，不影响pending状态
                readMessageBodyByRange(streamKey, pm.getId(), detail);

                details.add(detail);
            }
        } catch (Exception e) {
            log.error("Failed to query pending details from stream {}", streamKey, e);
        }
        return details;
    }

    /**
     * 通过 XRANGE range 只读读取单条消息体。
     * XRANGE 不涉及消费组，仅从 Stream 中按ID读取，不产生 ACK/待处理状态变化。
     */
    private void readMessageBodyByRange(String streamKey, RecordId messageId, PendingMessageDetail detail) {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .range(streamKey, Range.closed(messageId.getValue(), messageId.getValue()));

            if (records != null && !records.isEmpty()) {
                Map<Object, Object> body = records.get(0).getValue();
                detail.setOrderId(String.valueOf(body.getOrDefault("orderId", "")));
                detail.setUserId(String.valueOf(body.getOrDefault("userId", "")));
                detail.setVoucherId(String.valueOf(body.getOrDefault("voucherId", "")));
            }
        } catch (Exception e) {
            log.warn("Failed to read body via XRANGE for messageId={}", messageId.getValue(), e);
        }
    }
}
