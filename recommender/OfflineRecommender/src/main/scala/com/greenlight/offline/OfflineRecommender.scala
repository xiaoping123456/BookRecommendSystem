package com.greenlight.offline

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.jblas.DoubleMatrix

// 基于评分数据的LFM, 只需要UserRating数据
case class BookRating(uid: Int, bid: Int, score: Double)

case class MongoConfig(uri:String, db:String)

// 定义一个基准推荐对象
case class Recommendation(bid: Int, score_avg: Double)

// 定义基于预测评分的用户推荐列表
case class UserRecs(uid: Int, recs:Seq[Recommendation])

// 定义基于LFM书籍特征向量的书籍相似度列表
case class BookRecs(bid: Int, recs:Seq[Recommendation])

object OfflineRecommender {
  // 定义表名和常量
  val MONGODB_RATING_COLLECTION = "Rating"

  val USER_RECS = "UserRecs"
  val BOOK_RECS = "BookRecs"
  val USER_MAX_RECOMMENDATION = 20

  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/Book_Recommend",  // Book_Recommend 数据库
      "mongo.db" -> "Book_Recommend"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OfflineRecommender")

    // 创建一个sparkSession对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 从mongo加载数据  .cache做缓存
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[BookRating]
      .rdd
      .map( rating => (rating.uid, rating.bid, rating.score) )
      .cache()

    // 从RatingRDD中提取所有uid和bid，并去重
    val userRDD = ratingRDD.map(_._1).distinct().cache()
    val bookRDD = ratingRDD.map(_._2).distinct().cache()

    // 训练隐语义模型
    val trainData = ratingRDD.map( x => Rating(x._1, x._2, x._3))

    /**
     * rank:隐特征向量的维度
     * iterations:迭代次数
     * lambda:正则化系数
     * blocks:分解成几个并行计算的单元
     */
    val (rank, iterations, lambda) = (200, 5, 0.1)
    val model = ALS.train(trainData, rank, iterations, lambda)

    // 基于用户和图书的隐特征，计算预测评分，得到用户的推荐列表
    // 计算user和book的笛卡儿积，的到一个空评分矩阵
    val userBooks = userRDD.cartesian(bookRDD)

    // 调用model的predict方法预测评分
    val preRatings = model.predict(userBooks)

    val userRecs = preRatings
        .filter(_.rating > 0)       // 过滤出评分大于0的项
        .map(rating => (rating.user, (rating.product, rating.rating)))
        .groupByKey()
        .map{
          case (uid, recs) => UserRecs(uid, recs.toList.sortWith(_._2>_._2).
            take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1, x._2)))
        }
        .toDF()

    // 存入Mongo
    userRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", USER_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    // 基于图书隐特征计算相似度矩阵，得到图书相似推荐列表
    val bookFeatures = model.productFeatures.map{
      case (bid, features) => (bid, new DoubleMatrix(features))
    }

    // 对所有图书两两计算他们的相似度，做笛卡儿积
    val bookRecs = bookFeatures.cartesian(bookFeatures)
        .filter{
          case (a, b) => a._1!=b._1     // 把自己和自己的配对过滤, 去掉1
        }
        .map{
          case (a, b) => {
            val simScore = this.consinSim(a._2, b._2)
            (a._1, (b._1, simScore))    // 放在最后一行就是返回值
          }
        }
        .filter(_._2._2>0.6)            // 过滤出相似度大于0.6的
        .groupByKey()
        .map{
          case (bid, items) => BookRecs(bid, items.toList.sortWith(_._2>_._2).map(x => Recommendation(x._1, x._2)))
        }
        .toDF()

    // 存入Mongo
    bookRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", BOOK_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    spark.stop()
  }

  // 求向量余弦相似度
  def consinSim(book1: DoubleMatrix, book2: DoubleMatrix): Double = {
    book1.dot(book2) / (book1.norm2() * book2.norm2())
  }

}
