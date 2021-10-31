package com.sdut.springboot.model.request;

public class HotRecommendationRequest {
    int sum;

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public HotRecommendationRequest(int sum) {
        this.sum = sum;
    }
}
