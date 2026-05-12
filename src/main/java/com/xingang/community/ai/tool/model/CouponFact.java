package com.xingang.community.ai.tool.model;

public class CouponFact {

    private Long voucherId;
    private String title;
    private Integer discountAmount;
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
