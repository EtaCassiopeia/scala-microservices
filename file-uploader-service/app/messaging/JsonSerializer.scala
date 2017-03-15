package messaging

import java.util

import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import play.api.libs.json.{Json, Writes}

/**
  * JsonSerializer is a subclass of {@see org.apache.kafka.common.serialization.Serializer} to serialize a value
  * to Json format
  */
class JsonSerializer[A: Writes] extends Serializer[A] {

  private val stringSerializer = new StringSerializer

  override def configure(configs: util.Map[String, _], isKey: Boolean) =
    stringSerializer.configure(configs, isKey)

  override def serialize(topic: String, data: A) =
    stringSerializer.serialize(topic, Json.stringify(Json.toJson(data)))

  override def close() =
    stringSerializer.close()

}
