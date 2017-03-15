package transfer

import java.io.{File, FileInputStream}
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import java.util.concurrent.TimeUnit
import java.util.zip.{ZipEntry, ZipInputStream}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, StreamConverters}
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import dedup.ImplicitConversions._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}
import repository.InMemoryRepository

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class FileTransformer(config: Config, repository: InMemoryRepository, dataBot: ActorRef)
                     (implicit system: ActorSystem) extends LazyLogging {

  import system.dispatcher

  private val importDirectory = Paths.get(config.getString("transformer.tmp.dir")).toFile
  private val dateTimeFormatter= DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss").withOffsetParsed()

  def parseLine(filePath: String)(line: String): Future[Option[Record]] = Future {
    val fields = line.split(",")
    try {
      val id = fields(0).toInt
      val name = fields(1).toLowerCase
//      val timeOfStart = DateTime.parse(fields(2),dateTimeFormatter)
//        .withZone(DateTimeZone.UTC)
//        .toString()
val timeOfStart = fields(2)
      val obs = fields(3)
      Some(Record(id, name, timeOfStart, obs))
    } catch {
      case t: Throwable =>
        logger.error(s"Unable to parse line in $filePath:\n$line: ${t.getMessage}")
        None
    }
  }

  def executeBulkInsert(records: Seq[Record]) =
    repository.batchInsertIntoDb(records)

  val lineDelimiter: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"), 256, allowTruncation = true)

  val parseFile: Flow[File, Option[Record], NotUsed] =
    Flow[File].flatMapConcat { file =>

      val zipInputStream = new ZipInputStream(new FileInputStream(file))

      var entry: ZipEntry = zipInputStream.getNextEntry

      while ( entry.getName.contains("/") || !entry.getName.toLowerCase.contains(".csv"))
        entry = zipInputStream.getNextEntry

      logger.info(s"parsing ${entry.getName} part of ${file.getPath}")

      StreamConverters.fromInputStream(() => zipInputStream)
        .via(lineDelimiter)
        .drop(1) //drop header line
        .map(_.utf8String)
        .mapAsync(parallelism = 8)(parseLine(file.getPath))
    }

  def transformFile(fileName: String) = {
    implicit val materializer = ActorMaterializer()

    val startTime = System.currentTimeMillis()

    val list = (fileName :: Nil).filter(f => new java.io.File(s"$importDirectory/$f").exists)

    Source(list)
      .map(f => new File(s"$importDirectory/$f"))
      .via(parseFile)
      .filter(_.isDefined)
      .map(_.get)
      .withUniqueId(dataBot) {
        r => r.id
      }
      .grouped(100)
      .throttle(elements = 2,
        per = FiniteDuration(10, TimeUnit.SECONDS),
        maximumBurst = 2,
        mode = ThrottleMode.Shaping)
      .mapAsyncUnordered(2)(executeBulkInsert)
      .withAttributes(ActorAttributes.supervisionStrategy { e =>
        logger.error("Exception thrown during stream processing", e)
        Supervision.Resume
      })
      .runWith(Sink.ignore)
      .andThen {
        case Success(_) =>
          val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
          logger.info(s"Import finished in ${elapsedTime}s")
        case Failure(e) => logger.error("Import failed", e)
      }
  }
}
