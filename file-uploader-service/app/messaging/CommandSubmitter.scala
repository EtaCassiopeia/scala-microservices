package messaging

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.typesafe.config.Config
import messages.{LoadFileCommand, RowKey}
import org.apache.kafka.common.serialization.StringSerializer

class CommandSubmitter(config: Config) {

  private val producer = KafkaProducer(
    KafkaProducer.Conf(
      config,
      keySerializer = new StringSerializer,
      valueSerializer = new JsonSerializer[LoadFileCommand])
  )

  private val topic = config.getString("topic")

  def submit(rowKey: RowKey, loadFileCommand: LoadFileCommand) = producer.send(
    KafkaProducerRecord(topic, rowKey.id.toString, loadFileCommand)
  )

  def close() = producer.close()

}