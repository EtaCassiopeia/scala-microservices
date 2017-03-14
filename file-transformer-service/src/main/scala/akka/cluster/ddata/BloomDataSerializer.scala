package akka.cluster.ddata

import akka.actor.ExtendedActorSystem
import akka.cluster.ddata.protobuf.SerializationSupport
import akka.cluster.ddata.serialization.BloomFilterProto
import akka.serialization.Serializer
import com.googlecode.javaewah.EWAHCompressedBitmap
import com.twitter.algebird._

import scala.collection.immutable.BitSet
import scala.util.{Failure, Success, Try}

class BloomDataSerializer(val system: ExtendedActorSystem)
  extends Serializer with SerializationSupport {
  override def identifier: Int = 1970

  override def toBinary(obj: AnyRef): Array[Byte] = {
    obj match {
      case m: BloomDataType[_] => bloomFilterToPB(m.bloom).toByteArray
      case _ => throw new IllegalArgumentException(
        s"Can't serialize object of type ${obj.getClass}")
    }
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    new BloomDataType(bloomFilterFromBinary(bytes))
  }


  def bloomFilterToPB[A <: Int](bf: BF[A]): BloomFilterProto = {
    bf match {
      case bf@BFZero(hashes, width) =>
        BloomFilterProto(numHashes = hashes.size, width = width)
      case bf@BFItem(item, hashes, width) =>
        BloomFilterProto(item = item, numHashes = hashes.size, width = width)
      case bf@BFInstance(hashes, bits, width) =>
        BloomFilterProto(numHashes = hashes.size, denseBitset = bits.toSeq, width = width)
      case bf@BFSparse(hashes, cbits, width) =>
        BloomFilterProto(numHashes = hashes.size, sparseBitSet = cbits.toArray, width = width)
    }
  }

  def bloomFilterFromBinary[A <: Int](bytes: Array[Byte]): BF[A] = {
    val msg = BloomFilterProto.parseFrom(bytes)
    invert(msg).getOrElse(_empty).asInstanceOf[BF[A]]
  }

  def invert[A <: Int](pbf: BloomFilterProto): Try[BF[A]] = {
    val hashes = BFHash[A](pbf.numHashes, pbf.width)(Hash128.intHash)

    pbf match {
      case BloomFilterProto(_, _, _, width, _, denseBitSet) if denseBitSet.nonEmpty =>
        Success(BFInstance(hashes, BitSet(denseBitSet: _*), width))
      case BloomFilterProto(_, _, _, width, sparseBitSet, _) if sparseBitSet.nonEmpty =>
        Success(BFSparse(hashes, EWAHCompressedBitmap.bitmapOf(sparseBitSet: _*), width))
      case BloomFilterProto(item, _, _, width, _, _) =>
        Success(BFItem(item.asInstanceOf[A], hashes, width))
      case BloomFilterProto(_, _, _, width, _, _) =>
        Success(BFZero(hashes, width))
      case _ => Failure(new Exception("invalid bloom filter"))
    }
  }

  private def _empty[A <: Int]: BF[A] = {
    val fpProb = 0.01
    val dataSetSize = 1000000
    val width = BloomFilter.optimalWidth(dataSetSize, fpProb)
    val numOfHash = BloomFilter.optimalNumHashes(dataSetSize, width.getOrElse(10000))

    val bfMonoid = BloomFilterMonoid(numOfHash, width.getOrElse(10000))(Hash128.intHash)

    bfMonoid.empty.asInstanceOf[BF[A]]
  }
}