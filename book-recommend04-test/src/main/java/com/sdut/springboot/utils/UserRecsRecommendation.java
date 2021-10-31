package com.sdut.springboot.utils;

import java.util.List;
import java.util.Map;

public class UserRecsRecommendation {
    private int uid;
    private List<Map> recs;

    public int getBid() {
        return uid;
    }

    public void setBid(int uid) {
        this.uid = uid;
    }

    public List<Map> getRecs() {
        return recs;
    }

    public void setRecs(List<Map> recs) {
        this.recs = recs;
    }
}
