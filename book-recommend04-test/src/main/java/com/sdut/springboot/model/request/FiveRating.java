package com.sdut.springboot.model.request;

public class FiveRating {
    private Integer uid;
    private Integer bid;
    private Integer score;

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getBid() {
        return bid;
    }

    public void setBid(Integer bid) {
        this.bid = bid;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "FiveRating{" +
                "uid=" + uid +
                ", bid=" + bid +
                ", score=" + score +
                '}';
    }
}
