package domain.writeLog

import org.apache.spark.sql.SparkSession

object MainApp {

  def main(args: Array[String]): Unit = {

    var spark =
      SparkSession
        .builder()
        .config("spark.sql.streaming.checkpointLocation", "checkpoint")
        .appName("duc_local")
        .master("spark://spark-master:7077")
        .getOrCreate()


    var df = spark
      .readStream
      .format("kafka")
      .option(
        "kafka.bootstrap.servers",
        "http://172.20.0.5:9092"
      )
      .option(
        "auto.offset.reset", "latest"
      )
      .option(
        "subscribe",
        sys.env.getOrElse("KAFKA_TOPICS", "source_log")
      )
      .option(
        "group.id",
        "duc-tmp"
      )
      .load()

    df
      .writeStream
      .format("console")
      .start()
      .awaitTermination()
  }

}
