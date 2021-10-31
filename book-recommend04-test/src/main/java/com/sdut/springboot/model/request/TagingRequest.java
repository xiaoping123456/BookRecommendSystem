package com.sdut.springboot.model.request;

public class TagingRequest {
    private int uid;
    private int bid;
    private String tag;

    public TagingRequest(int uid, int bid, String tag) {
        this.uid = uid;
        this.bid = bid;
        this.tag = tag;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
