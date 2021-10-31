package com.sdut.springboot.utils;

import java.util.List;
import java.util.Map;

public class BookRecsRecommendation {
    private int bid;
    private List<Map> recs;

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public List<Map> getRecs() {
        return recs;
    }

    public void setRecs(List<Map> recs) {
        this.recs = recs;
    }
}
