package com.sdut.springboot.utils;

import java.util.List;
import java.util.Map;

public class TagsTopRecommendation {
    private String tags;
    private List<Map> recs;

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<Map> getRecs() {
        return recs;
    }

    public void setRecs(List<Map> recs) {
        this.recs = recs;
    }
}
