package controllers

import java.io._
import java.nio.file.{Files, Paths}

import akka.stream.scaladsl._
import akka.util.ByteString
import cakesolutions.kafka.KafkaConsumer
import cakesolutions.kafka.testkit.KafkaServer
import com.typesafe.config.ConfigFactory
import messages.LoadFileCommand
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class FileControllerSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterAll {

  private val kafkaServer = new KafkaServer(kafkaPort = 9093)

  private val baseConfig = ConfigFactory.parseString(
    s"""
       |{
       |  kafka.bootstrap.servers = "localhost:${kafkaServer.kafkaPort}"
       |  bootstrap.servers = "localhost:${kafkaServer.kafkaPort}"
       |}
       """.stripMargin)

  private val topic = "events"

  private val consumer = KafkaConsumer(KafkaConsumer.Conf(
    ConfigFactory.parseString(
      s"""
         |{
         |  topics = ["$topic"]
         |  group.id = "testing-group}"
         |  auto.offset.reset = "earliest"
         |}
          """.stripMargin).withFallback(baseConfig),
    keyDeserializer = new StringDeserializer(),
    valueDeserializer = new JsonDeserializer[LoadFileCommand]()
  ))

  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = kafkaServer.close()

  "FileController" must {
    "upload a file successfully" in {
      consumer.subscribe(List(topic).asJava)

      val tmpFile = java.io.File.createTempFile("prefix", "txt")
      tmpFile.deleteOnExit()
      val msg = "hello world"
      Files.write(tmpFile.toPath, msg.getBytes())

      val url = s"http://localhost:${Helpers.testServerPort}/upload"
      val responseFuture = ws.url(url).post(postSource(tmpFile))
      val response = await(responseFuture)
      response.status mustBe OK

      val records = consumer.poll(30.seconds.toMillis)
      records.count mustBe 1
      consumer.close()
    }
  }

  def postSource(tmpFile: File): Source[MultipartFormData.Part[Source[ByteString, _]], _] = {
    import play.api.mvc.MultipartFormData._
    Source(FilePart("name", "hello.txt", Option("text/plain"),
      FileIO.fromPath(Paths.get(tmpFile.getAbsolutePath))) :: DataPart("key", "value") :: List())
  }

  def ws = app.injector.instanceOf(classOf[WSClient])
}