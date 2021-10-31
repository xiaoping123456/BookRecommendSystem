package com.sdut.springboot.model.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value = "Taging")
public class Taging {

    @Id
    private String _id;

    private int uid;
    private int bid;
    private String tag;

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

    @Override
    public String toString() {
        return "Taging{" +
                "_id='" + _id + '\'' +
                ", uid=" + uid +
                ", bid=" + bid +
                ", tag='" + tag + '\'' +
                '}';
    }
}
