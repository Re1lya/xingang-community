package com.xingang.community.order.controller;

import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.exception.BusinessException;
import com.xingang.community.common.result.Result;
import com.xingang.community.order.dto.PendingListInfo;
import com.xingang.community.order.dto.SeckillStockVO;
import com.xingang.community.order.service.SeckillObservabilityService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀可观测接口（纯只读，仅供内部运维使用）。
 *
 * <h3>访问边界</h3>
 * <p>所有接口要求请求头 {@code X-Admin-Token} 与配置 {@code admin.token} 匹配，
 * 不匹配则返回 {@link ErrorCode#FORBIDDEN}。该机制不替代完整权限体系，
 * 仅作为运维接口的最低保护。</p>
 *
 * <h3>敏感数据说明</h3>
 * <p>{@code /pending} 返回的 userId/orderId 等字段为运维排查所需完整值，
 * 不得通过非管理员渠道暴露。后续增强可对 userId 做前缀脱敏。</p>
 *
 * <p>挂载在 /admin/seckill 路径下，与 /voucher-order/seckill 用户接口完全隔离。
 * 所有接口均为 GET 只读操作，不改变 Redis Stream 消息状态、不修改库存。</p>
 */
@RestController
@RequestMapping("/admin/seckill")
public class SeckillAdminController {

    @Value("${admin.token:}")
    private String adminToken;

    @Resource
    private SeckillObservabilityService seckillObservabilityService;

    /**
     * 查询 stream.orders pending-list 快照。
     * 返回消息总数、最早/最晚消息ID、每条消息的deliveryCount和消息体。
     */
    @GetMapping("/pending")
    public Result<PendingListInfo> getPendingList(HttpServletRequest request) {
        checkAdminToken(request);
        PendingListInfo info = seckillObservabilityService.getPendingListInfo();
        return Result.ok(info);
    }

    /**
     * 查询单张秒杀券的当前Redis库存与数据库库存对照。
     */
    @GetMapping("/stock/{voucherId}")
    public Result<SeckillStockVO> getSeckillStock(@PathVariable Long voucherId,
                                                   HttpServletRequest request) {
        checkAdminToken(request);
        SeckillStockVO vo = seckillObservabilityService.getSeckillStock(voucherId);
        return Result.ok(vo);
    }

    /**
     * 查询所有秒杀券的库存对照。
     */
    @GetMapping("/stocks")
    public Result<List<SeckillStockVO>> getAllSeckillStocks(HttpServletRequest request) {
        checkAdminToken(request);
        List<SeckillStockVO> list = seckillObservabilityService.getAllSeckillStocks();
        return Result.ok(list);
    }

    private void checkAdminToken(HttpServletRequest request) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "运维接口未配置 admin.token，禁止访问。请设置环境变量 ADMIN_TOKEN");
        }
        String token = request.getHeader("X-Admin-Token");
        if (token == null || !token.equals(adminToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问运维接口");
        }
    }
}
