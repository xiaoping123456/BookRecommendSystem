package com.sdut.springboot.utils;

public class BookRated {

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

    private Double score;   //用户对其的评分
    private boolean rated;    //判断用户是否已经被评分

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
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

    public String getPublish_time() {
        return publish_time;
    }

    public void setPublish_time(String publish_time) {
        this.publish_time = publish_time;
    }

    public String getB_intro_short() {
        return b_intro_short;
    }

    public void setB_intro_short(String b_intro_short) {
        this.b_intro_short = b_intro_short;
    }

    public Double getScore_avg() {
        return score_avg;
    }

    public void setScore_avg(Double score_avg) {
        this.score_avg = score_avg;
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

    public boolean isRated() {
        return rated;
    }

    public void setRated(boolean rated) {
        this.rated = rated;
    }

    @Override
    public String toString() {
        return "BookRated{" +
                "bid=" + bid +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", pic_url='" + pic_url + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publish_time='" + publish_time + '\'' +
                ", b_intro_short='" + b_intro_short + '\'' +
                ", score_avg=" + score_avg +
                ", b_intro='" + b_intro + '\'' +
                ", author_intro='" + author_intro + '\'' +
                ", score=" + score +
                ", rated=" + rated +
                '}';
    }
}
