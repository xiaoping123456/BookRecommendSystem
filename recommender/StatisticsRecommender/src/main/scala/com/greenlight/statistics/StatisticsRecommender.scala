package com.greenlight.statistics

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

case class Book(bid: Int, name: String, author: String, author_intro: String, pic_url: String, publisher: String,
                b_intro_short: String, b_intro: String)

case class Tag(bid: Int, publish_time: String, eva_num: Int, score_avg: Double, tags: String)

case class MongoConfig(uri:String, db:String)

// 定义一个基准推荐对象
case class Recommendation(bid: Int, score_avg: Double)

// 定义电影类别top10-推荐对象 (类别名，[bid, score])
case class TagsRecommendation(tags: String, recs:Seq[Recommendation])

object StatisticsRecommender {
  // 定义表名
  val MONGODB_BOOK_COLLECTION = "Book"
  val MONGODB_TAG_COLLECTION = "Tag"

  // 统计表的名称
  val RATE_MORE_BOOKS = "HighRateBooks"                     // 历史热门统计：评论+评分
  val RATE_MORE_RECENTLY_BOOKS = "RecentlyHighRateBooks"    // 新书热门统计：评论+评分+时间
  val AVERAGE_BOOKS = "ScoreHighBooks"                      // 高评分统计：评分
  val GENRES_TOP_BOOKS = "TagsTopBooks"                     // 类别统计

  def main(args: Array[String]): Unit = {
    // 定义配置信息
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/Book_Recommend",
      "mongo.db" -> "Book_Recommend"
    )

    // 创建一个sparkConf对象
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StatisticsRecommeder")

    // 创建一个sparkSession对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //从mongoDB加载数据
    val tagDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_TAG_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Tag]
      .toDF()

    val bookDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_BOOK_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Book]
      .toDF()

    // 创建名为ratings的视图
    tagDF.createOrReplaceTempView("ratings")

    // TODO: 不同的统计推荐结果
    // 1. 历史热门统计，历史评分最多
    val rateMoreBooksDF = spark.sql("select bid, eva_num, score_avg from ratings where score_avg > 8.5 order by eva_num DESC, score_avg DESC")
    // 把结果写入对应的mongodb表中
    storeDFInMongoDB(rateMoreBooksDF, RATE_MORE_BOOKS)

    // 2. 近期热门统计，按照"yyyy-MM"格式选取最近的
    // 创建一个日期格式化工具
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM");
    // 注册udf, 把时间格式统一
    spark.udf.register("changeDate", (x: String) => simpleDateFormat.format(simpleDateFormat.parse(x.replace(" ",""))))

    // 对原始数据预处理, 形成视图
    val ratingOfYearMonth = spark.sql("select bid, score_avg, eva_num, changeDate(publish_time) as yearmonth from ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfYearMonth")

    // 从ratingOfYearMonth中查找图书在最新年份的书籍, bid, eva_num, yearmonth
    val rateMoreRecentlyBooksDF = spark.sql("select bid, score_avg, eva_num, yearmonth from ratingOfYearMonth order by yearmonth desc, score_avg desc")

    // 存入mongodb
    storeDFInMongoDB(rateMoreRecentlyBooksDF, RATE_MORE_RECENTLY_BOOKS)

    // 3. 优质图书推荐，统计电影平均评分
    val averageBooksDF = spark.sql("select bid, score_avg from ratings order by score_avg DESC")
    storeDFInMongoDB(averageBooksDF, AVERAGE_BOOKS)

    // 4. 各类别电影Top统计
    // 定义所有类别
    val tags = List("小说","文学","经典","散文","名著","心理学","哲学","经济学","社会学","政治学","国学",
      "文化","历史","思想","人文","科技","艺术","流行","音乐","美术","科普","军事","近代史","传记","回忆录",
      "武侠","科幻","悬疑","奇幻","推理","青春","爱情","成长","生活","女性","教育","美食","健康","管理","理财")

    // 为做笛卡儿积，把tags转成RDD
    val tagsRDD = spark.sparkContext.makeRDD(tags)

    // 计算类别 Top10 , 首先对类别和电影做笛卡儿积
    val tagsTopBooksDF = tagsRDD.cartesian(tagDF.rdd)
        .filter{
          // 条件过滤，找出movie的字段genres值(ActionF|Adventrue) 包含当前类别genre(Action)的那些
          case (tag, bookRow) => bookRow.getAs[String]("tags").toLowerCase.contains( tag.toLowerCase )
        }
        .map{                      // key - value
          case (tag, bookRow) => (tag, (bookRow.getAs[Int]("bid"), bookRow.getAs[Double]("score_avg")))
        }
        .groupByKey()      // 根据类别聚合
        .map{
          // 转换数据结构  _._2 第二个元素
          case (tag, items) => TagsRecommendation (tag, items.toList.sortWith(_._2>_._2).take(15)
          .map(item => Recommendation(item._1, item._2)))
        }
        .toDF()

    storeDFInMongoDB(tagsTopBooksDF, GENRES_TOP_BOOKS)


    spark.stop()
  }

  def storeDFInMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit = {
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

  }

}
