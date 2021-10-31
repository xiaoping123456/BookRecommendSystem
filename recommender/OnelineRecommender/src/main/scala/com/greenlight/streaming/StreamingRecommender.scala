package com.greenlight.streaming

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

// 定义连接助手对象，序列化
object ConnHelper extends Serializable {
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient( MongoClientURI("mongodb://localhost:27017/Book_Recommend"))
}

case class MongoConfig(uri:String, db:String)

// 定义一个基准推荐对象
case class Recommendation(bid: Int, score_avg: Double)

// 定义基于预测评分的用户推荐列表
case class UserRecs(uid: Int, recs:Seq[Recommendation])

// 定义基于LFM书籍特征向量的书籍相似度列表
case class BookRecs(bid: Int, recs:Seq[Recommendation])

object StreamingRecommender {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_BOOKS_NUM = 20
  val MONGODB_STREAM_RECS_COLLECTION = "StreamRecs"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_BOOK_RECS_COLLECTION = "BookRecs"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/Book_Recommend",  // Book_Recommend 数据库
      "mongo.db" -> "Book_Recommend",
      "kafka.topic" -> "Book_Recommend"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OnlineRecommender")

    // 创建一个sparkSession对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    // 拿到streaming context
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2))    // batch duration:批处理时间

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 加载图书相似度矩阵数据
    // 进行广播
    val simBookMatrix = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_BOOK_RECS_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[BookRecs]
      .rdd
      .map { bookRecs =>    // 转换成map便于查询相似度
        (bookRecs.bid, bookRecs.recs.map(x => (x.bid, x.score_avg)).toMap)
      }.collectAsMap()
    // 定义广播变量
    val simBookMatrixBroadCast = sc.broadcast(simBookMatrix)

    // 定义kafka连接参数
    val kafkaParam = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "Book_Recommend",
      "auto.offset.reset" -> "latest"
    )
    // 通过kafka创建一个DStream
    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaParam)
    )

    // 把原始数据UID|BID|SCORE_AVG转换成评分流
    val ratingStream = kafkaStream.map{
      msg =>
        val attr = msg.value().split("\\|")
        ( attr(0).toInt, attr(1).toInt, attr(2).toDouble )
    }

    // 继续做流式处理，核心实时算法部分
    ratingStream.foreachRDD{
      rdds => rdds.foreach{
        case (uid, bid, score) => {
          println("Rating data coming! >>>>>>>>>>>>>>>>>>>>>>>>>>")
          // 1.从redis里获取当前用户最近的k次评分
          val userRecentlyRatings = getUserRecntlyRating(MAX_USER_RATINGS_NUM, uid, ConnHelper.jedis)

          try {
            // 2.从相似度矩阵中取出当前图书最相似的N个图书，作为备选列表，Array[bid]
            val candidateBooks = getTopSimBooks(MAX_SIM_BOOKS_NUM, bid, uid, simBookMatrixBroadCast.value)

            // 3.对每个备选图书，计算推荐优先级，得到当前用户的实时推荐列表，Array[(bid, score)]
            val streamRecs = computeBookScores(candidateBooks, userRecentlyRatings, simBookMatrixBroadCast.value)

            // 4.把推荐数据保存到mongodb
            saveDataToMongoDB(uid, streamRecs)
          }
          catch {
            case e:NoSuchElementException =>
              println("NoSuchElementException!");
          }
        }
      }
    }
    // 开始接受和处理数据
    ssc.start()
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>  Streaming started!")
    ssc.awaitTermination()
  }

  import scala.collection.JavaConversions._

  def getUserRecntlyRating(num: Int, uid: Int, jedis: Jedis): Array[(Int, Double)] = {
    // 从redis读取数据，用户评分数据保存在uid：UID为key的队列里，value是BID:SCORE
    jedis.lrange("uid:" + uid, 0, num-1)
      .map{
        item =>  // 具体每个评分又是以 : 分割的两个值
          val attr = item.split("\\:")
          ( attr(0).trim.toInt, attr(1).trim.toDouble )
      }
      .toArray
  }

  /**
   * 获取跟当前图书做相似的num个图书，作为备选图书
   * @param num       相似图书的数量
   * @param bid       当前图书ID
   * @param uid       当前评分用户ID
   * @param simBooks 相似度矩阵
   * @return          过滤之后的备选图书列表
   */
  def getTopSimBooks(num: Int, bid: Int, uid: Int, simBooks: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]])
                     (implicit mongoConfig: MongoConfig): Array[Int] = {
      // 1. 从相似度矩阵中达到所有相似的图书
      val allSimBooks = simBooks(bid).toArray
      // 2. 从mongodb中查询用户已看过的图书
      val ratingExist = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
        .find(MongoDBObject("uid" -> uid))
        .toArray
        .map{
          item => item.get("bid").toString.toInt
        }
      // 3. 把看过的过滤掉，得到输出列表
      allSimBooks.filter(x => ! ratingExist.contains(x._1))
        .sortWith(_._2 > _._2)
        .take(num)
        .map(x => x._1)
  }

  def computeBookScores(candidateBooks: Array[Int], userRecentlyRatings: Array[(Int, Double)],
                        simBooks: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Array[(Int, Double)] = {
    // 定义一个ArrayBuffer, 用于保存每一个备选图书的基础得分
    val scores = scala.collection.mutable.ArrayBuffer[(Int, Double)]()
    // 定义一个HashMap, 保存每一个备选图书的增强减弱因子
    val increMap = scala.collection.mutable.HashMap[Int, Int]()
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()

    for(candidateBooks <- candidateBooks; userRecentlyRatings <- userRecentlyRatings) {
      // 达到备选图书和最近评分图书的相似度
      val simScore = getBooksSimScore(candidateBooks, userRecentlyRatings._1, simBooks)

      if(simScore > 0.7) {
        // 计算备选图书的基础推荐得分
        scores += ( (candidateBooks, simScore * userRecentlyRatings._2) )
        if(userRecentlyRatings._2 > 3) {
          increMap(candidateBooks) = increMap.getOrDefault(candidateBooks, 0) + 1
        } else {
          decreMap(candidateBooks) = increMap.getOrDefault(candidateBooks, 0) + 1
        }
      }
    }

    // 根据备选图书的bid做groupBy， 根据公式计算最后的推荐评分
    scores.groupBy(_._1).map{
      // groupBy得到的数据 Map(bid -> ArrayBuffer[(bid, score)]
      case (bid, scoreList) =>
        (bid, scoreList.map(_._2).sum / scoreList.length + log(increMap.getOrDefault(bid, 1)) - log(decreMap.getOrDefault(bid, 1)))
    }.toArray.sortWith(_._2 > _._2)
  }

  // 获取俩个图书之间的相似度
  def getBooksSimScore(bid1: Int, bid2: Int, simBooks: scala.collection
  .Map[Int, scala.collection.immutable.Map[Int, Double]]): Double = {
    simBooks.get(bid1) match {
      case Some(sims) => sims.get(bid2) match {
        case Some(score_avg) => score_avg
        case None => 0.0
      }
      case None => 0.0
    }
  }

  // 取对数, 底数可变, 默认为10
  def log(m: Int): Double = {
    val N = 10
    math.log(m) / math.log(N)
  }

  def saveDataToMongoDB(uid: Int, streamRecs: Array[(Int, Double)])(implicit mongoConfig: MongoConfig): Unit = {
    // 定义到StreamRecs表的连接
    val streamRecsCollection = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_STREAM_RECS_COLLECTION)

    // 如果表中已有uid对应的数据，则删除
    streamRecsCollection.findAndRemove(MongoDBObject("uid" -> uid))

    // 将streamRecs数据存入表中
    streamRecsCollection.insert(MongoDBObject("uid"->uid,
      "recs"-> streamRecs.map(x=>MongoDBObject( "bid"->x._1, "score"->x._2))))
  }
}
