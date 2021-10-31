package com.sdut.springboot.model.domain;

public class BookOtherMessage {
    private int bid;
    private String publish_time;
    private double score_avg;

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public String getPublish_time() {
        return publish_time;
    }

    public void setPublish_time(String publish_time) {
        this.publish_time = publish_time;
    }

    public double getScore_avg() {
        return score_avg;
    }

    public void setScore_avg(double score_avg) {
        this.score_avg = score_avg;
    }
}
