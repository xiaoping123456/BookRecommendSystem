package com.sdut.springboot.model.request;

public class RecentlyHotRecommendationRequest {
    int sum;

    public RecentlyHotRecommendationRequest(int sum) {
        this.sum = sum;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
