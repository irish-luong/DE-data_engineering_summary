package domain.writeLog

import org.apache.spark.sql.SparkSession

import domain.GenerateMessage.Generator

object MainApp {

  def main(args: Array[String]): Unit = {


    var generator = new Generator();
    generator.run();
    println("Done generate some records")

    var spark =
      SparkSession
        .builder()
        .config("spark.sql.streaming.checkpointLocation", "checkpoint")
        .appName("duc_local")
        .master("local[*]")
        .getOrCreate()


    var df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "source_log")
      .option("startingOffsets", "earliest")
      .load()

    df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
    df.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "sink_log")
      .start()
      .awaitTermination()
  }

}
