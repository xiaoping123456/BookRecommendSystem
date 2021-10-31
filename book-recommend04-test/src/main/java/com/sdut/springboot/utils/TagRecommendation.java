package com.sdut.springboot.utils;

public class TagRecommendation {
    private int bid;
    private double score_avg;
    private String tags;

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public double getScore_avg() {
        return score_avg;
    }

    public void setScore_avg(double score_avg) {
        this.score_avg = score_avg;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
