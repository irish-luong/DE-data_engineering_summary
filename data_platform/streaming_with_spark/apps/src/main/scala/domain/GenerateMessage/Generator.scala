package domain.GenerateMessage


import adapter.kafka.Producer
import org.apache.kafka.clients.producer.ProducerRecord
object Generator {

  def main(args: Array[String]): Unit = {

    var config = Map[String, String](
      "bootstrap.servers" -> sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
      "key.serializer" -> "org.apache.kafka.common.serialization.StringSerializer",
      "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer",
      "ack" -> "all"
    )

    var topic = "source_log"
    var producer = new Producer(config = config).get_producer()

    try{

      for(i <- 1 to 10) {
        var record = new ProducerRecord[String, String](topic, i.toString, "record number " + i.toString)
        var metadata = producer.send(record);

        printf(
          s"Send record(%s, %s) to partition %d at offset %s \n",
          record.key(),
          record.value(),
          metadata.get().partition(),
          metadata.get().offset()
        )
      }

    } catch {
      case e:Exception => e.printStackTrace()
    } finally {
      producer.close()
    }
    for(i <- 0 to 15) {

    }
  }
}
