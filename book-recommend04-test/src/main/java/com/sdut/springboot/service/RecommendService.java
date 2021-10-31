package com.sdut.springboot.service;

import com.sdut.springboot.model.domain.Book;
import com.sdut.springboot.model.request.*;
import com.sdut.springboot.utils.*;
import com.sdut.springboot.model.recom.Recommendation;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringStack;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TransportClient esClient;

    @Autowired
    private BookService bookService;

    //模拟实时推荐算法
    public List<Integer> getStreamRecommendations2(StreamRecommendationRequest streamRecommendationRequest){

        try {
            Query query = new Query(Criteria.where("uid").is(streamRecommendationRequest.getUid()));
            query.limit(5);
            query.with(Sort.by(
                    Sort.Order.desc("timestamp")
            ));
            //获得用户最近的5次评分数据
            List<FiveRating> fiveRatings = mongoTemplate.find(query, FiveRating.class, "Rating");
            //存储score,order
            List<WiMessage> wiList = new ArrayList<>();
            //order最开始等于5 代表最近的一次评分
            int order = 5;
            for (FiveRating fiveRating : fiveRatings) {
//            System.out.println(fiveRating);
                //wi权重
                double wi;
                if (fiveRating.getScore() <= 3) {
                    wi = fiveRating.getScore() * 0.6;
                } else {
                    wi = fiveRating.getScore() * 0.6 + order * 0.4;
                }
                order--;

                WiMessage wiMessage = new WiMessage(fiveRating.getBid(), wi);
                wiList.add(wiMessage);
            }
            //按照wi降序排序
            Collections.sort(wiList);
            //计划每个bid对应的 要获取的相似图书的个数
            int num[] = {5, 5, 3, 2, 1};
            //获得要推荐的bid集合
            List<Integer> bids = new ArrayList<>();
            for (int i = 0; i < wiList.size(); i++) {
                Query query1 = new Query(Criteria.where("bid").is(wiList.get(i).getBid()));
                BookRecsRecommendation bookRecsRecommendation = mongoTemplate.findOne(query1, BookRecsRecommendation.class, "ContentBookRecs");
                List<Integer> ids = new ArrayList<>();
                List list = bookRecsRecommendation.getRecs();
                int len = list.size();
                for (int j = 0; j < num[i]; j++) {
                    if (j >= len)
                        break;
                    Map map = (Map) list.get(j);
                    int id = (int) map.get("bid");
                    ids.add(id);
                }
                bids.addAll(ids);
            }
            //随机抽取4个
            List<Integer> randoms = MyRandom.random(4, bids.size());
            List<Integer> bids_random = new ArrayList<>();
            for (Integer random : randoms) {
                bids_random.add(bids.get(random));
            }

            return bids_random;

        }catch (Exception e){
            return null;
        }
    }


    //混合推荐  实时6 + 内容4
    public List<Integer> getMixtureRecommendations(StreamRecommendationRequest request){
        //实时推荐推荐的集合
        List<Integer> streamIds = getStreamRecommendations(new StreamRecommendationRequest(request.getUid()));
        //根据实时推荐的bid 获取相应的根据内容推荐的 集合
        List<Integer> recsIds = new ArrayList<>();
        for(Integer streamId : streamIds){
            recsIds.addAll(getBookRecsBC(streamId));
        }
        List<Integer> mixtureIds = new ArrayList<>();
        for(int i=0;i<streamIds.size();i++){
            if (i>=6){
                break;
            }
            mixtureIds.add(streamIds.get(i));
        }
        List<Integer> randoms = MyRandom.random(4,recsIds.size());
        for (int random:randoms){
            mixtureIds.add(recsIds.get(random));
        }
        return mixtureIds;
    }

    //获取实时推荐的集合
    public List<Integer> getStreamRecommendations(StreamRecommendationRequest request){

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("uid").is(request.getUid()));
            UserRecsRecommendation userRecsRecommendation = mongoTemplate.findOne(query, UserRecsRecommendation.class, "StreamRecs");
            List list = userRecsRecommendation.getRecs();
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                if (i >= 10) {
                    break;
                }
                Map map = (Map) list.get(i);
                ids.add((Integer) map.get("bid"));
            }
            return ids;
        }catch (Exception e){
            return null;
        }

    }

    //获取热门的Recommendation的集合
    public List<Recommendation> getHotRecommendations(HotRecommendationRequest request){
        //获取前60本书
        Query query = new Query();
        query.limit(60);
        List<Recommendation> first_recommendations= mongoTemplate.find(query,Recommendation.class,"HighRateBooks");
        //从获取的60本书中随机抽取num本
        List<Integer> randoms = MyRandom.random(request.getSum(),60);
        List<Recommendation> recommendations = new ArrayList<>();
        for (Integer random : randoms){
            recommendations.add(first_recommendations.get(random));
        }
        return recommendations;
    }
    //获取近期热门的Recommendation的集合
    public List<Recommendation> getRecentlyHotRecommendations(RecentlyHotRecommendationRequest request){
        //获取前60本书
        Query query = new Query();
        query.limit(60);
        List<Recommendation> first_recommendations= mongoTemplate.find(query,Recommendation.class,"RecentlyHighRateBooks");
        //从获取的60本书中随机抽取num本
        List<Integer> randoms = MyRandom.random(request.getSum(),60);
        List<Recommendation> recommendations = new ArrayList<>();
        for (Integer random : randoms){
            recommendations.add(first_recommendations.get(random));
        }
        return recommendations;
    }
    //获取评分最高的
    public List<Recommendation> getHighScoreRecommendations(HighScoreRecommendationRequest request){
        //获取前60本书
        Query query = new Query();
        query.limit(60);
        List<Recommendation> first_recommendations= mongoTemplate.find(query,Recommendation.class,"ScoreHighBooks");
        //从获取的60本书中随机抽取num本
        List<Integer> randoms = MyRandom.random(request.getSum(),60);
        List<Recommendation> recommendations = new ArrayList<>();
        for (Integer random : randoms){
            recommendations.add(first_recommendations.get(random));
        }
        return recommendations;
    }

    //获取相似图书 协同过滤
    public List<Integer> getBookRecs(int bid){
        try {
            Query query = new Query(Criteria.where("bid").is(bid));
            BookRecsRecommendation bookRecsRecommendation = mongoTemplate.findOne(query, BookRecsRecommendation.class, "BookRecs");
            List<Integer> ids = new ArrayList<>();
            List list = bookRecsRecommendation.getRecs();
            int len = list.size();
            for (int i = 0; i < len; i++) {
                if (i >= 4)
                    break;
                Map map = (Map) list.get(i);
                int id = (int) map.get("bid");
                ids.add(id);
            }
            return ids;
        }catch (Exception e){
            return null;
        }
    }
    //获取相似图书 根据内容
    public List<Integer> getBookRecsBC(int bid){

        try {
            Query query = new Query(Criteria.where("bid").is(bid));
            BookRecsRecommendation bookRecsRecommendation = mongoTemplate.findOne(query, BookRecsRecommendation.class, "ContentBookRecs");
            List<Integer> ids = new ArrayList<>();
            List list = bookRecsRecommendation.getRecs();
            int len = list.size();
            for (int i = 0; i < len; i++) {
                if (i >= 4)
                    break;
                Map map = (Map) list.get(i);
                int id = (int) map.get("bid");
                ids.add(id);
            }
            return ids;
        }catch (Exception e){
            return null;
        }
    }

    //获取所有标签
    public List getGenres(){
        Query query = new Query();
        List<TagsTopRecommendation> tagsTopRecommendations =mongoTemplate.find(query, TagsTopRecommendation.class,"TagsTopBooks");
        List<String> genres = new ArrayList<>();
        for (TagsTopRecommendation tagsTopRecommendation : tagsTopRecommendations){
            genres.add(tagsTopRecommendation.getTags());
        }
        return genres;
    }
    //获取用户喜欢的每个标签的前15
    public List<Integer> getGenresTopBooks(List<String> genres){
        List<Integer> ids = new ArrayList<>();
        for (String genre:genres){
            Query query = new Query(Criteria.where("tags").is(genre));
            List<TagsTopRecommendation> tagsTopRecommendations =mongoTemplate.find(query, TagsTopRecommendation.class,"TagsTopBooks");

            for(TagsTopRecommendation tagsTopRecommendation : tagsTopRecommendations){
                List list = tagsTopRecommendation.getRecs();
                for (int i=0;i<list.size();i++){
                    Map map= (Map) list.get(i);
                    int id = (int) map.get("bid");
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    //搜索 (书名类似的)
//    public List search(SearchRecommendationRequest request){
//        MultiMatchQueryBuilder query = QueryBuilders.multiMatchQuery(request.getText(), "name", "b_intro_short","author");
//        return parseESResponse(esClient.prepareSearch().setIndices("book_recommend").setTypes("Book").setQuery(query).execute().actionGet());
//    }
//
//    private List<Recommendation> parseESResponse(SearchResponse response) {
//        List<Recommendation> recommendations = new ArrayList<>();
//        for (SearchHit hit : response.getHits()) {
//            recommendations.add(new Recommendation((int) hit.getSourceAsMap().get("bid")));
//        }
//        return recommendations;
//    }
    public List search(SearchRecommendationRequest request){
        Query query = new Query();
//        query.addCriteria(Criteria.where("name").regex(".*?"+request.getText().toString()+".*"));
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("name").regex(".*?"+request.getText().toString()+".*"),Criteria.where("author").regex(".*?"+request.getText().toString()+".*"));
        query.addCriteria(criteria);
        //查询到的总数
        Integer count = (int) mongoTemplate.count(query,Book.class,"Book");
        //查询到的书（规定数量）
        query.skip((request.getCurrentPage()-1)*request.getPageSize());
        query.limit(request.getPageSize());
        List<Book> books = mongoTemplate.find(query, Book.class,"Book");
        List<Integer> bookIds = new ArrayList<>();
        for(Book book:books){
            bookIds.add(book.getBid());
        }
        List<Book> nowBooks = bookService.getBooks(bookIds);
        HashMap<String,Object> mapBooks = new HashMap<>();
        mapBooks.put("books",nowBooks);

        List<Map<String,Object>> map = new ArrayList<Map<String,Object>>();

        HashMap<String,Object> mapCount = new HashMap<>();
        mapCount.put("count",count);
        HashMap<String,Object> mapCurrentPage = new HashMap<>();
        mapCurrentPage.put("currentPage",request.getCurrentPage());

        map.add(mapCount);
        map.add(mapCurrentPage);
        map.add(mapBooks);

        return map;

    }

    //根据标签搜索
    public List searchByTag(TagSearchRecommendationRequest request){
//        Query query = new Query(Criteria.where("tags").regex(".*?\\" +text+ ".*"));
        Query query = new Query(Criteria.where("tags").regex(request.getText()));
        Integer count = (int) mongoTemplate.count(query,TagRecommendation.class,"Tag");
        query.skip((request.getCurrentPage()-1)*request.getPageSize());
        query.limit(request.getPageSize());
        List<TagRecommendation> tagRecommendations = mongoTemplate.find(query,TagRecommendation.class,"Tag");

        List<Integer> ids = new ArrayList<>();
        for (TagRecommendation tagRecommendation:tagRecommendations){
            ids.add(tagRecommendation.getBid());
        }

        List<Map<String,Object>> map = new ArrayList<Map<String,Object>>();
        HashMap<String,Object> mapCount = new HashMap<>();
        mapCount.put("count",count);
        HashMap<String,Object> mapCurrentPage = new HashMap<>();
        mapCurrentPage.put("currentPage",request.getCurrentPage());

        HashMap<String,Object> mapIds = new HashMap<>();
        mapIds.put("ids",ids);

        map.add(mapCount);
        map.add(mapCurrentPage);
        map.add(mapIds);
        return map;
    }

}
