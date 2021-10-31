package com.sdut.springboot.controller;

import com.sdut.springboot.model.domain.Book;
import com.sdut.springboot.model.domain.Rating;
import com.sdut.springboot.model.domain.User;
import com.sdut.springboot.model.recom.Recommendation;
import com.sdut.springboot.model.request.*;
import com.sdut.springboot.service.*;
import com.sdut.springboot.utils.MyRandom;
import com.sdut.springboot.utils.RatingBook;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(value = "book")
public class BookController {

    private static Logger logger = Logger.getLogger(BookController.class.getName());

    @Autowired
    private BookService bookService;
    @Autowired
    private UserService userService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private TagingService tagingService;

    @Autowired
    private RatingService ratingService;

//    /**
//     * 【 混合推荐 】 实时6 内容4
//     * @param username
//     * @return
//     */
//    @RequestMapping("/mixture")
//    public List mixtureRecommend(@RequestParam("username")String username){
//        User user = userService.findByUsername(username);
//        List<Integer> ids = recommendService.getMixtureRecommendations(new StreamRecommendationRequest(user.getUid()));
//        return bookService.getBooks(ids);
//    }

    /**
     * 【 实时推荐 】
     * @param username
     * @return
     */
//    @RequestMapping("/stream")
//    public List streamRecommend(@RequestParam("username")String username){
//        User user = userService.findByUsername(username);
//        List<Integer> ids=recommendService.getStreamRecommendations(new StreamRecommendationRequest(user.getUid()));
//        return bookService.getBooks(ids);
//    }
    @RequestMapping("/stream")
    public List streamRecommend(@RequestParam("username")String username){
        User user = userService.findByUsername(username);
        List<Integer> ids=recommendService.getStreamRecommendations2(new StreamRecommendationRequest(user.getUid()));
        if (ids == null){
            return null;
        }
        return bookService.getBooks(ids);
    }


    /**
     * 根据标签搜索
     * @param text
     * @return
     */
    @RequestMapping("/searchByTag")
    public List searchByTag(@RequestParam("text")String text,
                            @RequestParam("currentPage")Integer currentPage,
                            @RequestParam("pageSize")Integer pageSize){
//        System.out.println(text+"    "+currentPage+"   "+pageSize);
        TagSearchRecommendationRequest tagSearchRecommendationRequest = new TagSearchRecommendationRequest(text,currentPage,pageSize);
        List map = recommendService.searchByTag(tagSearchRecommendationRequest);
        Map idsMap = (Map) map.get(2);
        List<Integer> ids = (List<Integer>) idsMap.get("ids");
        List<Book> books = bookService.getBooks(ids);
        Map<String,Object> mapBooks = new HashMap<>();
        mapBooks.put("books",books);
        map.add(mapBooks);
        map.remove(2);

        return map;
    }

    /**
     * 【 搜索 】
     * 用户在搜索框输入书籍名,作者类似的文字 在name , b_intro_short , author
     * @param text
     * @return  Recommendation对象的集合 包含bid
     */
    @RequestMapping("/search")
    public List search(@RequestParam("text")String text,
                       @RequestParam("currentPage")Integer currentPage,
                       @RequestParam("pageSize")Integer pageSize){
//        System.out.println(text);
        SearchRecommendationRequest searchRecommendationRequest = new SearchRecommendationRequest(text,currentPage,pageSize);
//        List<Recommendation> recommendations = recommendService.search(searchRecommendationRequest);
//        return recommendations;
        return recommendService.search(searchRecommendationRequest);
    }

    /**
     * 获取【 单一图书信息 】
     * @param bid
     * @return
     */
//    @RequestMapping("/info/{bid}")
//    public Book getBookInfo(@PathVariable("bid")int bid){
//        return bookService.findByBid(bid);
//    }
    @RequestMapping("/infoMessage")
    public List getBookInfoMessage(@RequestParam("bid")int bid){
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();

        HashMap map_book = new HashMap();
        map_book.put("book",bookService.findByBid(bid));
        HashMap map_recs1 = new HashMap();
        map_recs1.put("recs1",getBookRecs(bid));
        HashMap map_recs2 = new HashMap();
        map_recs2.put("recs2",getBookRecsBC(bid));

        list.add(map_book);
        list.add(map_recs1);
        list.add(map_recs2);
        return list;
    }


    /**
     * 推荐【 相似图书 】（bid） 最多推荐5个
     * 隐语义模型协同过滤
     * @param bid
     * @return
     */
    @RequestMapping("/bookRecs/{bid}")
    public List getBookRecs(@PathVariable("bid")int bid){
        List<Integer> ids = recommendService.getBookRecs(bid);
        List<Book> books = bookService.getBooks(ids);
        return books;
    }
    /**
     * 推荐【 相似图书 】（bid） 最多推荐5个
     * 根据内容
     * @param bid
     * @return
     */
    @RequestMapping("/bookRecsBC/{bid}")
    public List getBookRecsBC(@PathVariable("bid")int bid){
        List<Integer> ids = recommendService.getBookRecsBC(bid);
        List<Book> books = bookService.getBooks(ids);
        return books;
    }


    /**
     * 获取【 全部标签 】
     * @return
     */
    @RequestMapping("/genres")
    public List getAllGenres(){
        List genres = recommendService.getGenres();
        return genres;
    }

    /**
     * 获取【 用户喜欢看的 】
     * 根据用户选的喜欢的标签，从每个标签的前10中 一共推荐20个
     * @param username
     * @return
     */
    @RequestMapping("/wish")
    public List getUserLike(@RequestParam("username")String username){
        User user = userService.findByUsername(username);
        List<String> genres = user.getPrefGenres();
        List<Integer> ids = recommendService.getGenresTopBooks(genres);
//        System.out.println(ids);
        //随机获取4个
        List<Integer> randoms = MyRandom.random(4,ids.size());
        List<Integer> newIds = new ArrayList<>();
        for(Integer random:randoms){
            newIds.add(ids.get(random));
        }
        List<Book> books = bookService.getBooks(newIds);
        return books;
    }

    /**
     * 【 热门推荐 】
     * @param num
     * @return
     */
    @RequestMapping("/hot")
    public List getHotBooks(@RequestParam("num") int num){
        List<Recommendation> recommendations = recommendService.getHotRecommendations(new HotRecommendationRequest(num));
        List<Book> books = bookService.getRecommendBooks(recommendations);
        return books;
    }
    /**
     * 【 近期热门推荐 】
     * @param num
     * @return
     */
    @RequestMapping("/recentlyhot")
    public List getRecentlyHotBooks(@RequestParam("num")int num){
        List<Recommendation> recommendations = recommendService.getRecentlyHotRecommendations(new RecentlyHotRecommendationRequest(num));
        List<Book> books = bookService.getRecommendBooks(recommendations);
        return books;
    }
    /**
     * 【 经典推荐   评分最高的推荐 】
     * @param num
     * @return
     */
    @RequestMapping("/rate")
    public List getHighScoreBooks(@RequestParam("num")int num){
        List<Recommendation> recommendations = recommendService.getHighScoreRecommendations(new HighScoreRecommendationRequest(num));
        List<Book> books = bookService.getRecommendBooks(recommendations);
        return books;
    }
    /**
     * 【 获取用户评分过的书 】
     * @param username
     * @return
     */
    @RequestMapping("/myrate")
    public List getMyRateBooks(@RequestParam("username")String username){
        User user = userService.findByUsername(username);
        List<RatingBook> ratingBooks = bookService.getMyRateBooks(user.getUid());
        return ratingBooks;
    }

    /**
     * 用户给图书设标签
     * @param username
     * @param bid
     * @param tag
     * @return
     */
    @RequestMapping("/taging")
    public boolean insertTaging(@RequestParam("username")String username,@RequestParam("bid")int bid,@RequestParam("tag")String tag){
        int uid = userService.findByUsername(username).getUid();
        TagingRequest request = new TagingRequest(uid,bid,tag);
        return tagingService.insert(request);
    }

    /**
     * 用户给图书评分
     * @param username
     * @param bid
     * @param score
     * @return
     */
    @RequestMapping("/rating")
    public boolean insertRating(@RequestParam("bid")int bid,@RequestParam("username")String username,@RequestParam("score")double score){
        int uid = userService.findByUsername(username).getUid();
        long timestamp = System.currentTimeMillis();
        RatingRequest request = new RatingRequest(uid,bid,score,timestamp);


        try{
            boolean complete = ratingService.bookRating(request);
            //埋点日志
            if (complete){
                System.out.print("=========complete=========");
                logger.info("BOOK_RATING_PREFIX" + request.getUid() +"|"+ bid +"|"+ request.getScore() );
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 获取用户收藏的书
     * @param username
     * @return
     */
    @RequestMapping(value = "/myCollections")
    public List getMyCollections(@RequestParam("username")String username){
        User user = userService.findByUsername(username);
        List<Integer> bids = user.getCollectionBookBids();
        return bookService.getBookRated(bids,user.getUid());
    }

    /**
     * 判断用户是否已经收藏这本书
     * @param username
     * @param bid
     * @return
     */
    @RequestMapping(value = "/isCollected")
    public boolean isCollected(@RequestParam("username")String username,@RequestParam("bid")int bid){
        User user = userService.findByUsername(username);
        return bookService.isCollected(user,bid);
    }

//    /**
//     * 判断用户是否已经评分这本书
//     * 若已经被评分，则返回值为 用户对其评的分
//     * 若返回值为-1，则未被评分过
//     * @param username
//     * @param bid
//     * @return
//     */
//    @RequestMapping(value = "/isRated")
//    public Double isRated(@RequestParam("username")String username,@RequestParam("bid")int bid){
//        User user = userService.findByUsername(username);
//        return bookService.isRated(user,bid);
//    }

    /**
     * 收藏图书
     * @param username
     * @param bid
     * @return
     */
    @RequestMapping(value = "/collect")
    public boolean collectBook(@RequestParam("username")String username,@RequestParam("bid")int bid){
        System.out.println(username+"    "+bid);
        User user = userService.findByUsername(username);
        List<Integer> collections = user.getCollectionBookBids();
        for (Integer collection:collections){
            if(collection==bid)
                return false;
        }

        user.getCollectionBookBids().add(bid);

        return userService.updateCollectionBookBids(user);
    }

    /**
     * 取消收藏
     * @param username
     * @param bid
     * @return
     */
    @RequestMapping(value = "/cancelCollect")
    public boolean cancelCollect(@RequestParam("username")String username,@RequestParam("bid")int bid){
        User user = userService.findByUsername(username);

        return userService.cancelCollect(user,bid);
    }

    //根据bid查询
    @RequestMapping(value = "/findByBid")
    public Book findByBid(int bid){
        Book book = bookService.findByBid(bid);
        return book;
    }
    //根据name查询
    @RequestMapping(value = "/findByName")
    public Book findByName(String name){
        Book book = bookService.findByName(name);
        return book;
    }

}
