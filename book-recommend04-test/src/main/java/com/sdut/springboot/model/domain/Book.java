package com.sdut.springboot.model.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(value = "Book")
public class Book {

    @Id
    private String _id;     //主键

    private Integer bid;    //book的id
    private String name;   //书名
    private String author;  //作者
    private String pic_url; //缩略图链接
    private String publisher;   //出版社
    private String publish_time;    //出版时间
    private String b_intro_short;   //短简介
    private Double score_avg;   //评分
    private String b_intro;     //内容简介
    private String author_intro;    //作者简介

    public String getPublish_time() {
        return publish_time;
    }

    public void setPublish_time(String publish_time) {
        this.publish_time = publish_time;
    }

    public Double getScore_avg() {
        return score_avg;
    }

    public void setScore_avg(Double score_avg) {
        this.score_avg = score_avg;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Integer getBid() {
        return bid;
    }

    public void setBid(Integer bid) {
        this.bid = bid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPic_url() {
        return pic_url;
    }

    public void setPic_url(String pic_url) {
        this.pic_url = pic_url;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getB_intro_short() {
        return b_intro_short;
    }

    public void setB_intro_short(String b_intro_short) {
        this.b_intro_short = b_intro_short;
    }

    public String getB_intro() {
        return b_intro;
    }

    public void setB_intro(String b_intro) {
        this.b_intro = b_intro;
    }

    public String getAuthor_intro() {
        return author_intro;
    }

    public void setAuthor_intro(String author_intro) {
        this.author_intro = author_intro;
    }

    @Override
    public String toString() {
        return "Book{" +
                "_id='" + _id + '\'' +
                ", bid=" + bid +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", pic_url='" + pic_url + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publish_time='" + publish_time + '\'' +
                ", b_intro_short='" + b_intro_short + '\'' +
                ", score_avg=" + score_avg +
                ", b_intro='" + b_intro + '\'' +
                ", author_intro='" + author_intro + '\'' +
                '}';
    }
}
