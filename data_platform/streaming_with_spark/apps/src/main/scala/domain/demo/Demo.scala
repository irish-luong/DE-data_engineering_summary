package domain.demo

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}


object Demo {
  def main(args: Array[String]): Unit = {

    println("Hello");
    var spark =
      SparkSession
        .builder
        .appName("Demo")
        .master(sys.env.getOrElse("SPARK_MASTER", "local[*]"))
        .config("spark.eventLog.enabled", value = false)
        .getOrCreate()

    var rdd = spark.sparkContext.parallelize(List(Row("Hello"), Row("World")))

    var schema = StructType(
      List(
        StructField("value", StringType)
      )
    )
    var df = spark.createDataFrame(rdd, schema)

    df.show()
    spark.stop()
  }
}
