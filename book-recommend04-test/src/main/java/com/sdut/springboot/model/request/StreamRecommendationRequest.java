package com.sdut.springboot.model.request;

public class StreamRecommendationRequest {
    private Integer uid;

    public StreamRecommendationRequest(Integer uid) {
        this.uid = uid;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }
}
