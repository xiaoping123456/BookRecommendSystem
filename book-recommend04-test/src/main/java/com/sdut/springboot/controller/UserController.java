package com.sdut.springboot.controller;

import com.sdut.springboot.model.domain.User;
import com.sdut.springboot.model.request.LoginUserRequest;
import com.sdut.springboot.model.request.RegisterUserRequest;
import com.sdut.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    //注册
    @RequestMapping(value = "/register")
    public boolean register(@RequestParam("username")String username,
                            @RequestParam("password")String password,
                            @RequestParam("genres") String genres){
        RegisterUserRequest registerUserRequest = new RegisterUserRequest(username,password);

        if(userService.checkUserExist(username)) {
            return false;
        }else{
            userService.register(registerUserRequest);
            User user = userService.findByUsername(username);
            user.getPrefGenres().addAll(Arrays.asList(genres.split(",")));
            user.setFirst(false);
            userService.updateGenres(user);

            return true;
        }
    }


    //登录
    @RequestMapping(value = "/login")
    public User login(@RequestParam("username")String username,@RequestParam("password")String password){
        System.out.println(username+"====="+password);
        LoginUserRequest loginUserRequest = new LoginUserRequest(username,password);
        User user = userService.login(loginUserRequest);
        return user;
    }

//    @RequestMapping(value = "/login")
//    public User login(Map<String,Object> map){
//        String username = (String) map.get("username");
//        String password = (String) map.get("password");
//
//        System.out.println(username+"====="+password);
//        LoginUserRequest loginUserRequest = new LoginUserRequest(username,password);
//        User user = userService.login(loginUserRequest);
//        return user;
//    }

//    //冷启动问题 (添加用户喜欢的标签)
//    @RequestMapping(value = "/pref", produces = "application/json")
//    @ResponseBody
//    public Boolean addPrefGenres(@RequestParam("username") String username, @RequestParam("genres") String genres) {
////        if(!userService.checkUserExist(username)){
////            return false;
////        }
//        System.out.println(username);
//        System.out.println(genres);
//        User user = userService.findByUsername(username);
//        user.getPrefGenres().addAll(Arrays.asList(genres.split(",")));
//        user.setFirst(false);
//        return userService.updateGenres(user);
//    }


    //注销，删除uesr
    @RequestMapping(value = "/remove")
    public boolean remove(@RequestParam("username")String username){
        int uid = findByUsername(username).getUid();
        if(userService.remove(uid))
            return true;
        else
            return false;

    }
    //修改密码
    @RequestMapping(value = "/updatePassword")
    public User updatePassword(@RequestParam("username")String username,@RequestParam("password")String password){
        int uid = findByUsername(username).getUid();
        return userService.updatePassword(uid,password);
    }

    //根据uid查询
    @RequestMapping(value = "/findByUid")
    public User findByUid(@RequestParam("uid") int uid){
        User user= userService.findByUid(uid);
        return user;
    }
    //根据username查询
    @RequestMapping(value = "/findByUsername")
    public User findByUsername(@RequestParam("username") String username){
        User user= userService.findByUsername(username);
        return user;
    }
}
