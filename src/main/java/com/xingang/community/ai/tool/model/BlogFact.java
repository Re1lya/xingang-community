package com.xingang.community.ai.tool.model;

public class BlogFact {

    private Long blogId;
    private String title;
    private Integer likedCount;
    private String summary;

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(Integer likedCount) {
        this.likedCount = likedCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
