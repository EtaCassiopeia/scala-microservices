package akka.cluster.ddata

import com.twitter.algebird._

object BloomDataType {
  private val _empty: BloomDataType[Int] = {
    val fpProb = 0.01 //false positive rate
    val dataSetSize = 1000000
    val width = BloomFilter.optimalWidth(dataSetSize, fpProb)
    val numOfHash = BloomFilter.optimalNumHashes(dataSetSize, width.getOrElse(10000))

    val bfMonoid = BloomFilterMonoid(numOfHash, width.getOrElse(10000))(Hash128.intHash)

    new BloomDataType[Int](bfMonoid.empty)
  }

  def empty[A <: Int]: BloomDataType[A] = _empty.asInstanceOf[BloomDataType[A]]

  def apply(): BloomDataType[Int] = _empty

  /**
    * Java API
    */
  def create[A <: Int](): BloomDataType[A] = empty[A]

}

/**
  * Implements a 'Bloom Filter' CRDT, also called a 'BloomDataType'.
  * A Bloom filter is a space-efficient probabilistic data structure, that is used to test whether an element
  * is a member of a set.
  *
  * This class is immutable, i.e. "modifying" methods return a new instance.
  */
@SerialVersionUID(1L)
final class BloomDataType[A <: Int](val bloom: BF[A]) extends ReplicatedData with ReplicatedDataSerialization with FastMerge {

  type T = BloomDataType[A]

  override def merge(that: BloomDataType[A]): BloomDataType[A] = {
    if ((this eq that) || that.isAncestorOf(this)) this.clearAncestor()
    else if (this.isAncestorOf(that)) that.clearAncestor()
    else {
      clearAncestor()
      new BloomDataType[A](bloom ++ that.bloom)
    }
  }

  def contains(a: A): Boolean = {
    bloom.contains(a).isTrue
  }

  /**
    * Adds an element to the bloom filter
    */
  def +(element: A): BloomDataType[A] = add(element)

  def add(element: A): BloomDataType[A] = assignAncestor(new BloomDataType[A](bloom + element))

  override def toString: String = s"BloomDataType(${bloom.toString})"

  override def equals(o: Any): Boolean = o match {
    case other: BloomDataType[A] => bloom.equals(other.bloom)
    case _ => false
  }

  override def hashCode: Int = bloom.hashCode()

}


object BloomFilterKey {
  def create[A <: Int](id: String): Key[BloomDataType[A]] = BloomFilterKey(id)
}

@SerialVersionUID(1L)
final case class BloomFilterKey[A <: Int](_id: String) extends Key[BloomDataType[A]](_id) with ReplicatedDataSerialization
