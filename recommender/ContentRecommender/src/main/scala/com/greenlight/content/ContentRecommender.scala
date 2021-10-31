package com.greenlight.content

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

// 需要的数据源是图书的Tag数据库
case class Tag(bid: Int, publish_time: String, eva_num: Int, score_avg: Double, tags: String)

case class MongoConfig(uri:String, db:String)

// 定义一个基准推荐对象
case class Recommendation(bid: Int, score_avg: Double)

// 定义基于图书内容信息提取出的特征向量的书籍相似度列表
case class BookRecs(bid: Int, recs:Seq[Recommendation])

object ContentRecommender {
  // 定义表名和常量
  val MONGODB_TAG_COLLECTION = "Tag"
  val CONTENT_BOOK_RECS = "ContentBookRecs"

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

    // 加载数据，并作预处理
    val bookTagsDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_TAG_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Tag]
      .map(
        // 提取bid, tags作为内容特征 分词器默认按照空格分词
        x => (x.bid, x.tags.split('|').take(3).mkString(" "))
      )
      .toDF("bid", "tags")
      .cache()


    // 核心部分：用TF-IDF从内容信息中提取电影特征向量
    // 创建一个分词器，默认按照空格分词
    val tokenizer = new Tokenizer().setInputCol("tags").setOutputCol("key_words")

    // 用分词器对原始数据做转换，生成新的一列words
    val wordsData = tokenizer.transform(bookTagsDF)

    // 引入HashingTF工具，可以把一个词语转化成对应的词频   NumFeatures:哈希分统署特征数量
    val hashingTF = new HashingTF().setInputCol("key_words").setOutputCol("rawFeatures").setNumFeatures(100)
    val featurizedData = hashingTF.transform(wordsData)

    // 引入IDF工具, 可以得到idf模型
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    // 训练idf模型, 得到每个词的逆文档频率
    val idfModel = idf.fit(featurizedData)

    // 用模型对原数据进行处理, 得到每个词的tf-idf, 作为新的特征向量
    val rescaledData = idfModel.transform(featurizedData)

    rescaledData.show(truncate = false)

    val bookFeatures = rescaledData.map {
      row => (row.getAs[Int]("bid"), row.getAs[SparseVector]("features").toArray)
    }
      .rdd
      .map(
        x => (x._1, new DoubleMatrix(x._2))
      )
    bookFeatures.collect().foreach(println)

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
      .option("collection", CONTENT_BOOK_RECS)
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
