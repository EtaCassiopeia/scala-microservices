package messaging

import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.ActorMaterializer
import cakesolutions.kafka.KafkaProducer
import cakesolutions.kafka.testkit.KafkaServer
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, WordSpec}

import scala.concurrent.Future

class EventConsumerSpec extends WordSpec with BeforeAndAfterAll with Matchers {
  private val kafkaServer = new KafkaServer(kafkaPort = 9094)

  private val baseConfig = ConfigFactory.parseString(
    s"""
       |{
       |  kafka.bootstrap.servers = "localhost:${kafkaServer.kafkaPort}"
       |  bootstrap.servers = "localhost:${kafkaServer.kafkaPort}"
       |}
       """.stripMargin)

  private val topic = "events"

  private val producer = KafkaProducer(KafkaProducer.Conf(keySerializer = new StringSerializer,
    valueSerializer = new StringSerializer,
    bootstrapServers = s"localhost:${kafkaServer.kafkaPort}"))

  private var callCounter = 0

  private val actorSystem = ActorSystem("test", ConfigFactory.load("test"))
  implicit val materializer = ActorMaterializer.create(actorSystem)

  private val mockEventHandler = new EventHandler {
    override def processMessage(msg: Message): Future[CommittableMessage[Array[Byte], String]] = {
      callCounter = callCounter + 1
      Future.successful(msg)
    }

    override val system: ActorSystem = actorSystem
  }

  actorSystem.actorOf(Props(new EventConsumer(mockEventHandler)))

  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = {
    kafkaServer.close()
    actorSystem.terminate()
  }

  "EventConsumer" must {
    "handle an event successfully" in {
      producer.send(new ProducerRecord("key1", "value"))

      Thread.sleep(2000)

      callCounter should be(1)
    }
  }

}
