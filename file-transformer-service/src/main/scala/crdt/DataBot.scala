package crdt

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._

import scala.concurrent.duration._

object DataBot {

  case class Put(value: Integer)

  case class MightContain(value: Int)

}

/**
  * DataBot is an Actor which is responsible for handling BloomFilter related request.
  *
  */
class DataBot extends Actor with ActorLogging {

  import DataBot._

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  val DataKey = BloomFilterKey[Int]("id")

  val readMajority = ReadMajority(timeout = 5.seconds)
  val writeMajority = WriteMajority(timeout = 5.seconds)
  val readAll = ReadAll(timeout = 5.seconds)
  val writeAll = WriteAll(timeout = 5.seconds)

  replicator ! Subscribe(DataKey, self)

  def receive = {
    case Put(value) =>
      log.info("Adding: {}", value)
      replicator ! Update(DataKey, BloomDataType.empty[Int], writeMajority)(_ + value)
    case MightContain(value) =>
      log.info("Check for value: {}", value)
      replicator ! Get(DataKey, readMajority, request = Some((value, sender())))
    case g@GetSuccess(DataKey, Some((value: Int, replyTo: ActorRef))) =>
      replyTo ! g.get(DataKey).contains(value)
    case GetFailure(DataKey, Some((value: Int, replyTo: ActorRef))) =>
      log.info("Get Failure for value: {}", value)
      replyTo ! false
    case NotFound(DataKey, Some((value: Int, replyTo: ActorRef))) =>
      log.info("NotFound for value: {}", value)
      replyTo ! false

    case _: UpdateResponse[_] => // ignore

    case c@Changed(DataKey) =>
      val data = c.get(DataKey)
      log.info("Current elements: {}", data.bloom.numBits)
  }

}