package messaging

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ ConsumerMessage, ConsumerSettings, Subscriptions }
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }

object EventSource {

  def create(bootstrapServers:String,groupId: String)(implicit system: ActorSystem): Source[ConsumerMessage.CommittableMessage[Array[Byte], String], Consumer.Control] = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer.committableSource(consumerSettings, Subscriptions.topics(Events.Topic))
  }
}
