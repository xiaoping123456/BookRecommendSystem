package com.sdut.springboot.service;

import com.sdut.springboot.model.domain.Rating;
import com.sdut.springboot.model.request.RatingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class RatingService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Jedis jedis;


    public boolean bookRating(RatingRequest request) {
        Rating rating = new Rating(request.getUid(), request.getBid(), request.getScore());
        rating.setTimestamp(request.getTimestamp());
        updateRedis(rating);
        if (ratingExist(rating.getUid(), rating.getBid())) {
            return updateRating(rating);
        } else {
            return newRating(rating);
        }
    }
    //更新mongo中的rating数据
    private boolean updateRating(Rating rating){
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").is(rating.getUid()));
        query.addCriteria(Criteria.where("bid").is(rating.getBid()));
        Update update = new Update();
        update.set("score",rating.getScore());
        try{
            mongoTemplate.updateFirst(query,update,"Rating");
            return true;
        }catch(Exception e){
            return false;
        }
    }
    //把rating插入mongo中
    private boolean newRating(Rating rating){
        try{
            mongoTemplate.save(rating,"Rating");
            return true;
        }catch (Exception e){
            return false;
        }
    }
    //把数据存入redis
    private void updateRedis(Rating rating) {
        if (jedis.exists("uid:" + rating.getUid()) && jedis.llen("uid:" + rating.getUid()) >= 40) {
            jedis.rpop("uid:" + rating.getUid());
        }
        jedis.lpush("uid:" + rating.getUid(), rating.getBid() + ":" + rating.getScore());
    }
    //rating是否存在
    private boolean ratingExist(int uid,int bid){
        return null != findRating(uid, bid);
    }
    //在mongodb中查询rating
    private Rating findRating(int uid,int bid){
        Query query = new Query(Criteria.where("uid").is(uid));
        query.addCriteria(Criteria.where("bid").is(bid));
        Rating rating = mongoTemplate.findOne(query,Rating.class,"Rating");
        return rating;
    }

}
