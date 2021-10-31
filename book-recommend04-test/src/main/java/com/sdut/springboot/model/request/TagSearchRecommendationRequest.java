package com.sdut.springboot.model.request;

public class TagSearchRecommendationRequest {
    private String text;
    private Integer currentPage;
    private Integer pageSize;

    public TagSearchRecommendationRequest(String text, Integer currentPage, Integer pageSize) {
        this.text = text;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
