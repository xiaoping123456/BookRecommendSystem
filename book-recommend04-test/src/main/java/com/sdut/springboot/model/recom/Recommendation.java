package com.sdut.springboot.model.recom;

public class Recommendation {
    private int bid;

    public int getBid() { return bid; }

    public void setBid(int bid) { this.bid = bid; }

    public Recommendation(int bid) {
        this.bid = bid;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "bid=" + bid +
                '}';
    }
}
