package com.sdut.springboot.model.request;

public class HighScoreRecommendationRequest {
    private int sum;

    public HighScoreRecommendationRequest(int sum) {
        this.sum = sum;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
