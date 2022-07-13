package writeLog

import org.apache.spark.sql.SparkSession

object MainApp {

  def main(args: Array[String]): Unit = {

    var spark =
      SparkSession
        .builder()
        .config("spark.sql.streaming.checkpointLocation", "checkpoint")
        .appName("duc_local")
        .master(sys.env.getOrElse("SPARK_MASTER", "local[*]"))
        .getOrCreate()


    var df = spark
      .readStream
      .format("kafka")
      .option(
        "kafka.bootstrap.servers",
        sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVER", "localhost:9092")
      )
      .option(
        "auto.offset.reset", "latest"
      )
      .option(
        "subscribe",
        sys.env.getOrElse("KAFKA_TOPICS", "source_log")
      )
      .load()

    df
      .writeStream
      .format("console")
      .start()
      .awaitTermination()
  }

}

//./bin/spark-submit  \
//  --master spark://localhost:7077 \
//  --deploy-mode client \
//  --driver-memory 1g \
//  --executor-memory 1g \
//  --executor-cores 1  \
//  --class writeLog.MainApp\
//  "/opt/spark-apps/target/scala-2.13/apps_2.13-0.1.jar"
