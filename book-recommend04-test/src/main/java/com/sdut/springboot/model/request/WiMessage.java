package com.sdut.springboot.model.request;

public class WiMessage implements Comparable<WiMessage> {
    private double wi;
    private int bid;

    public WiMessage(int bid, double wi) {
        this.wi = wi;
        this.bid = bid;
    }

    public double getWi() {
        return wi;
    }

    public void setWi(double wi) {
        this.wi = wi;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    @Override
    public int compareTo(WiMessage o) {
        return (int)(o.getWi()*10 - this.getWi()*10);
    }
}
