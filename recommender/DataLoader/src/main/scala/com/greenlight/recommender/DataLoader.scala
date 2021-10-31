package com.greenlight.recommender

import java.net.InetAddress

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

/**
 * Book 数据集
 *
 * 0                                                   图书ID：  bid
 * 人类简史                                             图书名称： name
 * [以色列] 尤瓦尔·赫拉利                                图书作者： author
 * 图书图片                                             图片链接： pic_url
 * 中信出版社                                           出版社：   publisher
 * 2014-11                                             出版时间： publish_time
 * 156922                                              评论数：   eva_num
 * 十万年前，地球上至少有六种不同的人但今日，                图书简介： b_intro_short
 * 9.1                                                 图书评分： score_avg
 * ……，世界舞台为什么只剩下了我们自己？                     图书介绍： b_intro
 * 尤瓦尔·赫拉利，1976年生                               作者简介： author_intro
 * 历史|人类简史|人类学|人类|科普|哲学|思维|文化            图书标签： tags
 **/
case class Book(bid: Int, name: String, author: String, author_intro: String, pic_url: String, publisher: String,
                b_intro_short: String, b_intro: String)

case class Tag(bid: Int, publish_time: String, eva_num: Int, score_avg: Double, tags: String)

case class Rating(uid: Int, bid: Int, score: Double)

// 把MongoDB和ES的配置封装成样例类
/**
 * @param uri   MongoDB连接
 * @param db    MongoDB数据库
 */
case class MongoConfig(uri:String, db:String)

/**
 *
 * @param httpHosts         http主机列表（，分隔）
 * @param transportHosts    transport主机列表
 * @param index             需要操作的索引
 * @param clustername       集群名称，默认elasticsearch
 */
case class ESConfig(httpHosts:String, transportHosts:String, index:String, clustername:String)

object DataLoader {

  // 定义常量
  val BOOK_DATA_PATH = "D:\\SDUT\\Code\\IDEA\\BookRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\Books.csv"
  val TAG_DATA_PATH = "D:\\SDUT\\Code\\IDEA\\BookRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\Tags.csv"
  val RATING_DATA_PATH = "D:\\SDUT\\Code\\IDEA\\BookRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\Ratings.csv"

  // 定义表名
  val MONGODB_BOOK_COLLECTION = "Book"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_TAG_COLLECTION = "Tag"
  val ES_BOOK_INDEX = "Book"

  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/Book_Recommend",  // Book_Recommend 数据库
      "mongo.db" -> "Book_Recommend",
      "es.httpHosts" -> "localhost:9200",
      "es.transportHosts" -> "localhost:9300",
      "es.index" -> "book_recommend",
      "es.cluster.name" -> "elasticsearch"
    )

    // 创建一个sparkConf对象
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")

    // 创建一个sparkSession对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    // 加载数据, CSV -> RDD -> DateFrame
    val bookRDD = spark.sparkContext.textFile(BOOK_DATA_PATH)
    val bookDF = bookRDD.map(
      item => {
        val attr = item.split(",")
        Book(attr(0).toInt, attr(1).trim, attr(2).trim, attr(3).trim,
          attr(4).trim, attr(5).trim, attr(6).trim, attr(7).trim)
      }
    ).toDF()

    val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)
    val tagDF = tagRDD.map(item => {
      val attr = item.split(",")
      Tag(attr(0).toInt, attr(1).trim, attr(2).toInt, attr(3).toDouble, attr(4).trim)
    }).toDF()

    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      Rating(attr(0).toInt, attr(1).toInt, attr(2).toDouble)
    }).toDF()

    // 隐式定义
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 将数据保存到MongoDB
    storeDataInMongoDB(bookDF, ratingDF, tagDF)

    // 定义隐式配置参数
    implicit val esConfig = ESConfig(config("es.httpHosts"), config("es.transportHosts"), config("es.index"), config("es.cluster.name"))

    // 保存到ES, 方便模糊查询
//    storeDataInES(bookDF)

    spark.stop()
  }

  def storeDataInMongoDB(bookDF: DataFrame, ratingDF: DataFrame, tagDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    // 首先新建一个mongodb的连接, 创建客户端
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    // 如果mongodb中已有相应的数据库，先删除
    mongoClient(mongoConfig.db)(MONGODB_BOOK_COLLECTION).dropCollection()

    //将DF数据写入对应的mongodb表中
    bookDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_BOOK_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    tagDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_TAG_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //对数据表建索引
    mongoClient(mongoConfig.db)(MONGODB_BOOK_COLLECTION).createIndex(MongoDBObject("bid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex(MongoDBObject("bid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("bid" -> 1))

    // 关闭mongodb连接
    mongoClient.close()
  }

  def storeDataInES(bookDF: DataFrame)(implicit eSConfig: ESConfig): Unit = {
    // 新建es配置
    val settings: Settings = Settings.builder().put("cluster.name", eSConfig.clustername).build()

    // 新建一个es客户端
    val esClient = new PreBuiltTransportClient(settings)

    // .：任意字符，+：多个，\\d表示一个数字，
    val REGEX_HOST_PORT = "(.+):(\\d+)".r
    eSConfig.transportHosts.split(",").foreach{
      case REGEX_HOST_PORT(host: String, port:String) => {
        esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port.toInt))
      }
    }

    // 先清理遗留的数据
    if( esClient.admin().indices().exists(new IndicesExistsRequest(eSConfig.index))
      .actionGet()
      .isExists
    ){
      esClient.admin().indices().delete(new DeleteIndexRequest(eSConfig.index))
    }

    // 创建
    esClient.admin().indices().create(new CreateIndexRequest(eSConfig.index))

    bookDF.write
      .option("es.nodes", eSConfig.httpHosts)
      .option("es.http.timeout", "100m")
      .option("es.mapping.id", "bid")
      .mode("overwrite")
      .format("org.elasticsearch.spark.sql")
      .save(eSConfig.index + "/" + ES_BOOK_INDEX)
  }

}
