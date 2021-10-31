package com.sdut.springboot.service;

import com.sdut.springboot.model.domain.Book;
import com.sdut.springboot.model.domain.BookOtherMessage;
import com.sdut.springboot.model.domain.Rating;
import com.sdut.springboot.model.domain.User;
import com.sdut.springboot.model.recom.Recommendation;
import com.sdut.springboot.model.request.TagSearchRecommendationRequest;
import com.sdut.springboot.utils.BookRated;
import com.sdut.springboot.utils.RatingBook;
import com.sdut.springboot.utils.TagRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class BookService {

    @Autowired
    private MongoTemplate mongoTemplate;

    //根据bid查询
    public Book findByBid(int bid){
        Query query=new Query(Criteria.where("bid").is(bid));
        Book book = mongoTemplate.findOne(query,Book.class,"Book");
        BookOtherMessage message = mongoTemplate.findOne(query,BookOtherMessage.class,"Tag");
        book.setScore_avg(message.getScore_avg());
        book.setPublish_time(message.getPublish_time());
       return book;
    }
    //根据name查询
    public Book findByName(String name){
        Query query=new Query(Criteria.where("name").is(name));
        Book book = mongoTemplate.findOne(query,Book.class,"Book");
        return book;
    }

    //获取要得到的book集合
    public List<Book> getRecommendBooks(List<Recommendation> recommendations){
        List<Integer> bids = new ArrayList<>();
        for (Recommendation recommendation:recommendations){
            bids.add(recommendation.getBid());
        }
        return getBooks(bids);
    }
    //根据bid的集合查询book的集合
    public List<Book> getBooks(List<Integer> bids){

        try {
            List<Book> books = new ArrayList<>();
            for (Integer bid : bids) {
                Query query = new Query(Criteria.where("bid").is(bid));
                Book book = mongoTemplate.findOne(query, Book.class, "Book");

                BookOtherMessage message = mongoTemplate.findOne(query, BookOtherMessage.class, "Tag");
                book.setScore_avg(message.getScore_avg());
                book.setPublish_time(message.getPublish_time());
                books.add(book);
            }
            return books;
        }catch (Exception e){
            return null;
        }
    }

    //查询一个用户评分过的book集合
    public List<RatingBook> getMyRateBooks(int uid){
        Query query = new Query(Criteria.where("uid").is(uid));
        List<Rating> ratings = mongoTemplate.find(query,Rating.class,"Rating");
        List<Integer> bids = new ArrayList<>();
        for (Rating rating:ratings){
            bids.add(rating.getBid());
        }
        return getRatingBooks(bids,uid);
    }

    //返回用户评分过的书的全部信息，包括评分和时间戳
    public List<RatingBook> getRatingBooks(List<Integer> bids,int uid){
        List<RatingBook> ratingBooks = new ArrayList<>();
        for(Integer bid:bids){
            //获取图书信息
            Query query1 = new Query(Criteria.where("bid").is(bid));
            RatingBook ratingBook = mongoTemplate.findOne(query1,RatingBook.class,"Book");
            BookOtherMessage message = mongoTemplate.findOne(query1,BookOtherMessage.class,"Tag");
            ratingBook.setScore_avg(message.getScore_avg());
            ratingBook.setPublish_time(message.getPublish_time());
            //获取评分信息
            Query query2 = new Query(Criteria.where("bid").is(bid).and("uid").is(uid));
            Rating rating = mongoTemplate.findOne(query2,Rating.class,"Rating");

            ratingBook.setScore_user(rating.getScore());
            ratingBook.setTimestamp(rating.getTimestamp());
            ratingBooks.add(ratingBook);
        }
        return ratingBooks;
    }

    //判断这本书是否已经被用户收藏
    public boolean isCollected(User requestUser,int bid){
        Iterator<Integer> iterator = requestUser.getCollectionBookBids().iterator();
        while(iterator.hasNext()){
            Integer obj = iterator.next();
            if(obj==bid){
                return true;
            }
        }
        return false;
    }

    //判断这本书是否已经被用户评分
    public Double isRated(int uid,int bid){
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").is(uid).and("bid").is(bid));
        Rating rating = mongoTemplate.findOne(query,Rating.class,"Rating");
        if(rating==null){
            return -1.0;
        }else {
            return rating.getScore();
        }
    }

    //在book基础上加上用户是否已经被用户评分
    public List getBookRated(List<Integer> bids,int uid){
        List<BookRated> bookRateds = new ArrayList<>();
        for (Integer bid: bids){
            Query query = new Query(Criteria.where("bid").is(bid));
            BookRated bookRated = mongoTemplate.findOne(query,BookRated.class,"Book");

            BookOtherMessage message = mongoTemplate.findOne(query,BookOtherMessage.class,"Tag");
            bookRated.setScore_avg(message.getScore_avg());
            bookRated.setPublish_time(message.getPublish_time());

            if(isRated(uid,bid)<0){
                bookRated.setRated(false);
            }else {
                bookRated.setRated(true);
                bookRated.setScore(isRated(uid,bid));
            }
            bookRateds.add(bookRated);
        }
        return bookRateds;
    }

}
