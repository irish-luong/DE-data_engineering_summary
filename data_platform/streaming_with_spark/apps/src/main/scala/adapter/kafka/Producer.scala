package adapter.kafka

import scala.jdk.CollectionConverters._
import org.apache.kafka.clients.producer.KafkaProducer

import java.util

class Producer(config: Map[String, String]) {


  var props:util.Properties = new util.Properties();

  for((k, v) <- config) {
    props.put(k, v)
    println("Key: %s, Value: %s", k, v)
  }

  var producer = new KafkaProducer[String, String](props)

  def get_producer(): KafkaProducer[String, String] = {
    producer;
  }

}
