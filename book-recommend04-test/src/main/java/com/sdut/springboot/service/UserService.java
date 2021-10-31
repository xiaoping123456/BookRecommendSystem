package com.sdut.springboot.service;

import com.sdut.springboot.model.domain.User;
import com.sdut.springboot.model.request.LoginUserRequest;
import com.sdut.springboot.model.request.RegisterUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class UserService {

    @Autowired
    private MongoTemplate mongoTemplate;


    //根据uid查询
    public User findByUid(int uid){
        Query query=new Query();
        query.addCriteria(Criteria.where("uid").is(uid));
        User user =null;
        user = mongoTemplate.findOne(query,User.class,"User");
        return user;
    }
    //根据username查询
    public User findByUsername(String username){
        Query query =new Query();
        query.addCriteria(Criteria.where("username").is(username));
        User user = null;
        user = mongoTemplate.findOne(query,User.class,"User");

        return user;
    }
    //检查username是否存在 true(存在) false(不存在)
    public boolean checkUserExist(String username){
        if(findByUsername(username)==null)
            return false;
        else
            return true;
    }

    //注册
    public boolean register(RegisterUserRequest registerUserRequest){
        User user = new User();
        user.setUsername(registerUserRequest.getUsername());
        user.setPassword(registerUserRequest.getPassword());
        user.setTimestamp(System.currentTimeMillis());
        user.setFirst(true);

        //求uid
        Query query = new Query();
        int count= (int) mongoTemplate.count(query,"User");
        user.setUid(count+1);

        try{
            mongoTemplate.insert(user,"User");
            return true;
        }catch (Exception exception){
            return false;
        }
    }
    //登录
    public User login(LoginUserRequest loginUserRequest){
        User user = findByUsername(loginUserRequest.getUsername());
        if(user==null){
            return null;
        }else if(user.getPassword().equals(loginUserRequest.getPassword())){
            return user;
        }else{
            return null;
        }
    }
    //注销，删除user
    public boolean remove(int uid){
        Query query = new Query(Criteria.where("uid").is(uid));
        try{
            mongoTemplate.remove(query,User.class,"User");
            return true;
        }catch (Exception e){
            return false;
        }
    }
    //修改密码
    public User updatePassword(int uid,String password) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("password", password);
        //更新查询返回结果集的第一条
        mongoTemplate.updateFirst(query, update, User.class,"User");
        User user = findByUid(uid);
        return user;
    }
    //更新用户喜欢的标签以及first
    public boolean updateGenres(User requestUser){
        Query query = new Query(Criteria.where("uid").is(requestUser.getUid()));
        Update update = new Update().set("prefGenres",requestUser.getPrefGenres());
        Update update2 = new Update().set("first",requestUser.isFirst());
        mongoTemplate.updateFirst(query,update,User.class,"User");
        mongoTemplate.updateFirst(query,update2,User.class,"User");
        return true;
    }

    //更改用户的收藏集合 collectionBookBids
    public boolean updateCollectionBookBids(User requestUser){
        Query query = new Query(Criteria.where("uid").is(requestUser.getUid()));
        Update update = new Update().set("collectionBookBids",requestUser.getCollectionBookBids());
        try{
            mongoTemplate.updateFirst(query,update,User.class,"User");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    //取消收藏
    public boolean cancelCollect(User requestUser,int bid){
        Iterator<Integer> iterator= requestUser.getCollectionBookBids().iterator();
        while(iterator.hasNext()){
            Integer obj = iterator.next();
            if(obj==bid){
                iterator.remove();
            }
        }
        return updateCollectionBookBids(requestUser);
    }


}
