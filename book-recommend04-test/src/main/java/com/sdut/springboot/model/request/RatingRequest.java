package com.sdut.springboot.model.request;

public class RatingRequest {
    private int uid;
    private int bid;
    private double score;

    private long timestamp;

    public RatingRequest(int uid, int bid, double score, long timestamp) {
        this.uid = uid;
        this.bid = bid;
        this.score = score;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
