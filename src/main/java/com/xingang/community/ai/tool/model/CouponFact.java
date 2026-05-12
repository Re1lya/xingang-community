package com.xingang.community.ai.tool.model;

public class CouponFact {

    private Long voucherId;
    private String title;
    /**
     * Estimated savings in fen when actualValue >= payValue.
     * Kept for backward compatibility; prefer payValue/actualValue/valueDescription.
     */
    private Integer discountAmount;
    /**
     * Amount user needs to pay, unit: fen.
     */
    private Long payValue;
    /**
     * Face value / actual voucher value, unit: fen.
     */
    private Long actualValue;
    /**
     * Human-readable value semantics, avoiding ambiguous discount interpretation.
     */
    private String valueDescription;
    private Integer stock;
    private String validTimeRange;

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getPayValue() {
        return payValue;
    }

    public void setPayValue(Long payValue) {
        this.payValue = payValue;
    }

    public Long getActualValue() {
        return actualValue;
    }

    public void setActualValue(Long actualValue) {
        this.actualValue = actualValue;
    }

    public String getValueDescription() {
        return valueDescription;
    }

    public void setValueDescription(String valueDescription) {
        this.valueDescription = valueDescription;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getValidTimeRange() {
        return validTimeRange;
    }

    public void setValidTimeRange(String validTimeRange) {
        this.validTimeRange = validTimeRange;
    }
}
