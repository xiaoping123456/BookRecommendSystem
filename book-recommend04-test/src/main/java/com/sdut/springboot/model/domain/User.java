package com.sdut.springboot.model.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(value = "User")
public class User {

    @Id
    private String _id;     //主键

    private Integer uid;    //user的id
    private String username;    //用户名
    private String password;    //密码
    private List<String> prefGenres = new ArrayList<>();  //用户喜欢的标签

    private List<Integer> collectionBookBids = new ArrayList<>();     //用户收藏的书的bid集合

    private boolean first;      //判断是否以选择喜欢的标签

    private long timestamp;     //注册的时间戳

    public List<Integer> getCollectionBookBids() {
        return collectionBookBids;
    }

    public void setCollectionBookBids(List<Integer> collectionBookBids) {
        this.collectionBookBids = collectionBookBids;
    }

    public boolean isFirst() { return first; }

    public void setFirst(boolean first) { this.first = first; }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getPrefGenres() {
        return prefGenres;
    }

    public void setPrefGenres(List<String> prefGenres) {
        this.prefGenres = prefGenres;
    }

    public boolean confirmPassword(String password){
        return this.password.compareTo(password) == 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "_id='" + _id + '\'' +
                ", uid=" + uid +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", prefGenres=" + prefGenres +
                ", collectionBookBids=" + collectionBookBids +
                ", first=" + first +
                ", timestamp=" + timestamp +
                '}';
    }
}
