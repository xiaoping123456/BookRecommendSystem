package com.sdut.springboot.service;

import com.sdut.springboot.model.request.TagingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class TagingService {
    @Autowired
    private MongoTemplate mongoTemplate;

    //添加用户对图书的标签
    public boolean insert(TagingRequest request){
        try{
            mongoTemplate.save(request,"Taging");
            return true;
        }catch (Exception e){
            return false;
        }
    }

}
