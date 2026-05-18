package com.xingang.community.ai.agent;

import com.xingang.community.ai.agent.dto.RetrievalHit;
import com.xingang.community.ai.planning.AgentExecutionPlan;
import com.xingang.community.ai.tool.LocalLifeAgentTools;
import com.xingang.community.ai.tool.model.CouponFact;
import com.xingang.community.ai.tool.model.ShopCandidate;
import com.xingang.community.ai.tool.model.ShopDetailFact;
import com.xingang.community.ai.tool.model.ToolExecutionSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AnswerComposer {

    public String compose(AgentExecutionPlan plan, ToolExecutionSnapshot snapshot, List<RetrievalHit> retrievalHits) {
        if (plan == null) {
            return "我暂时还没解析出你的需求，请补充希望查询的商户或场景。";
        }
        ToolExecutionSnapshot safeSnapshot = snapshot == null ? new ToolExecutionSnapshot() : snapshot;
        return switch (normalizeIntent(plan.getIntent())) {
            case "recommendation" -> composeRecommendationAnswer(safeSnapshot, retrievalHits);
            case "fact_query" -> composeFactQueryAnswer(safeSnapshot, retrievalHits);
            default -> composeSupportAnswer(retrievalHits);
        };
    }

    private String composeRecommendationAnswer(ToolExecutionSnapshot snapshot, List<RetrievalHit> retrievalHits) {
        List<String> parts = new ArrayList<>();
        List<ShopCandidate> candidates = CollectionUtils.isEmpty(snapshot.getRecommendedCandidates())
                ? snapshot.getSearchedCandidates()
                : snapshot.getRecommendedCandidates();

        if (CollectionUtils.isEmpty(candidates)) {
            parts.add("暂时没有查到符合条件的店铺候选。");
        } else {
            parts.add("我先按你的条件整理了这些候选店铺：");
            for (int i = 0; i < Math.min(candidates.size(), 3); i++) {
                parts.add((i + 1) + ". " + formatCandidate(candidates.get(i)));
            }
        }

        parts.add(composeShopDetailText(snapshot));
        parts.add(composeCouponText(snapshot));
        parts.add(composeRagNotice(retrievalHits));
        return String.join("\n", parts);
    }

    private String composeFactQueryAnswer(ToolExecutionSnapshot snapshot, List<RetrievalHit> retrievalHits) {
        List<String> parts = new ArrayList<>();
        parts.add(composeShopDetailText(snapshot));
        parts.add(composeCouponText(snapshot));
        parts.add(composeRagNotice(retrievalHits));
        return String.join("\n", parts);
    }

    private String composeSupportAnswer(List<RetrievalHit> retrievalHits) {
        if (CollectionUtils.isEmpty(retrievalHits)) {
            return "我可以继续帮你查询门店、优惠券或平台规则，你可以告诉我更具体的问题。";
        }
        RetrievalHit hit = retrievalHits.get(0);
        if (StringUtils.hasText(hit.getSnippet())) {
            return "结合平台规则说明：" + hit.getSnippet();
        }
        return "结合平台规则说明：" + (StringUtils.hasText(hit.getTitle()) ? hit.getTitle() : "已命中相关知识内容。");
    }

    private String composeShopDetailText(ToolExecutionSnapshot snapshot) {
        if (!snapshot.wasExecuted(LocalLifeAgentTools.TOOL_GET_SHOP_DETAIL)) {
            return "本次未查询店铺详情。";
        }
        ShopDetailFact detail = snapshot.getShopDetail();
        Long selectedShopId = snapshot.getSelectedShopId();
        if (detail == null || detail.getShopId() == null) {
            if (selectedShopId == null) {
                return "已查询店铺详情，但当前缺少可用 shopId。";
            }
            return "已查询店铺详情，但暂未返回有效详情数据。";
        }
        List<String> fields = new ArrayList<>();
        fields.add("店铺详情：" + defaultText(detail.getName(), "未知店铺") + "（shopId=" + detail.getShopId() + "）");
        fields.add("人均" + formatFen(detail.getAveragePrice()));
        fields.add("评分" + (detail.getRating() == null ? "暂缺" : String.format(Locale.ROOT, "%.1f", detail.getRating())));
        fields.add("地址" + defaultText(detail.getAddress(), "暂缺"));
        fields.add("营业状态" + defaultText(detail.getBusinessStatus(), "UNKNOWN"));
        return String.join("，", fields);
    }

    private String composeCouponText(ToolExecutionSnapshot snapshot) {
        if (!snapshot.wasExecuted(LocalLifeAgentTools.TOOL_GET_SHOP_COUPONS)) {
            return "本次未查询优惠券信息。";
        }
        List<CouponFact> coupons = snapshot.getShopCoupons();
        Long selectedShopId = snapshot.getSelectedShopId();
        if (CollectionUtils.isEmpty(coupons)) {
            if (selectedShopId == null) {
                return "已查询店铺优惠券，但当前缺少可用 shopId。";
            }
            return "已查询该店铺优惠券，但当前未返回可用优惠券。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("可用优惠券：");
        for (int i = 0; i < Math.min(coupons.size(), 3); i++) {
            CouponFact coupon = coupons.get(i);
            lines.add((i + 1) + ". " + defaultText(coupon.getTitle(), "未命名优惠券") + "，"
                    + formatCouponValue(coupon)
                    + "，库存" + (coupon.getStock() == null ? "暂缺" : coupon.getStock())
                    + "，有效期" + defaultText(coupon.getValidTimeRange(), "暂缺"));
        }
        return String.join("\n", lines);
    }

    private String composeRagNotice(List<RetrievalHit> retrievalHits) {
        if (CollectionUtils.isEmpty(retrievalHits)) {
            return "未命中补充规则说明。";
        }
        RetrievalHit hit = retrievalHits.get(0);
        return "补充规则说明（RAG）：" + defaultText(hit.getTitle(), "已命中相关知识内容");
    }

    private String formatCandidate(ShopCandidate candidate) {
        if (candidate == null) {
            return "候选店铺信息暂缺";
        }
        List<String> fields = new ArrayList<>();
        fields.add(defaultText(candidate.getName(), "未知店铺") + "（shopId=" + defaultText(candidate.getShopId(), "N/A") + "）");
        fields.add("分类" + defaultText(candidate.getCategory(), "暂缺"));
        fields.add("人均" + formatFen(candidate.getAveragePrice()));
        fields.add("评分" + (candidate.getRating() == null ? "暂缺" : String.format(Locale.ROOT, "%.1f", candidate.getRating())));
        fields.add("距离" + (candidate.getDistanceKm() == null ? "暂缺" : String.format(Locale.ROOT, "%.2fkm", candidate.getDistanceKm())));
        return String.join("，", fields);
    }

    private String formatCouponValue(CouponFact coupon) {
        if (coupon == null) {
            return "金额信息暂缺";
        }
        if (StringUtils.hasText(coupon.getValueDescription())) {
            return coupon.getValueDescription();
        }
        if (coupon.getPayValue() != null || coupon.getActualValue() != null) {
            return "payValue=" + defaultText(coupon.getPayValue(), "N/A") + "分, actualValue="
                    + defaultText(coupon.getActualValue(), "N/A") + "分";
        }
        if (coupon.getDiscountAmount() != null) {
            return "预计优惠" + formatFen(coupon.getDiscountAmount());
        }
        return "金额信息暂缺";
    }

    private String normalizeIntent(String intent) {
        return StringUtils.hasText(intent) ? intent.trim().toLowerCase(Locale.ROOT) : "support";
    }

    private String formatFen(Integer valueInFen) {
        if (valueInFen == null) {
            return "暂缺";
        }
        return String.format(Locale.ROOT, "%.2f元", valueInFen / 100D);
    }

    private String defaultText(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof String value) {
            return StringUtils.hasText(value) ? value : fallback;
        }
        return String.valueOf(raw);
    }
}
