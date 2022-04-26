package japgolly.webapputil.general

import japgolly.univeq.UnivEq
import scala.jdk.CollectionConverters._

/** Equivalent to a `Map[String, List[String]]`.
  *
  * This is abstract for easy newtype creation.
  */
abstract class AbstractMultiStringMap[Self](final val asVector: Vector[(String, String)], final val isNormalised: Boolean) { self: Self =>

  final type Self2 = Self with AbstractMultiStringMap[Self]

  protected def create(asVector: Vector[(String, String)], isNormalised: Boolean = false): Self2

  protected def displayName: String =
    getClass.getSimpleName

  final def isEmpty  = asVector.isEmpty
  final def nonEmpty = asVector.nonEmpty

  def get(key: String): Vector[String] =
    asVector.iterator.filter(_._1 == key).map(_._2).toVector

  def add(key: String, value: String): Self =
    create(asVector :+ ((key, value)))

  def delete(key: String): Self =
    create(asVector.filter(_._1 != key))

  final def normalised: Self2 =
    if (isNormalised || asVector.isEmpty)
      this
    else
      normalise

  private lazy val normalise: Self2 = {
    // According to the Scala doc, this is a stable sort
    val result = asVector.sortBy(_._1)
    create(result, isNormalised = true)
  }

  override def toString =
    asVector
      .iterator
      .map(x => s"${x._1} -> ${x._2}")
      .mkString(displayName + "(", ", ", ")")

  override def hashCode =
    normalised.asVector.hashCode

  override def equals(that: Any) =
    that match {
      case t: AbstractMultiStringMap[_] => normalised.asVector == t.normalised.asVector
      case _                            => false
    }

  def filterKeys(retain: String => Boolean): Self = {
    val vec2 = asVector.filter(kv => retain(kv._1))
    if (vec2.length == asVector.length) this else create(vec2)
  }

  def whitelistKeys(whitelist: Set[String]): Self =
    filterKeys(whitelist.contains)

  def whitelistKeys(subset: Self2): Self = {
    val keys = subset.asVector.iterator.map(_._1).toSet
    whitelistKeys(keys)
  }

  def toMultiStringMap: MultiStringMap =
    new MultiStringMap(asVector, isNormalised = isNormalised)
}

object AbstractMultiStringMap {

  trait Module[A] {

    def fromVector(v: Vector[(String, String)]): A

    final val empty: A =
      fromVector(Vector.empty)

    final def apply(kvs: (String, String)*): A =
      fromVector(kvs.toVector)

    final def fromSeq(s: Seq[(String, String)]): A =
      fromVector(s.toVector)

    final def fromMap(m: Map[String, String]): A =
      fromVector(m.toVector)

    final def fromMultimap[C <: Iterable[String]](m: Map[String, C]): A =
      fromVector(
        m
          .iterator
          .flatMap { case (k, vs) => vs.iterator.map((k, _)) }
          .toVector
      )

    final def fromJavaMultimap[C <: java.util.Collection[String]](multimap: java.util.Map[String, C]): A =
      fromVector(
        multimap
          .entrySet()
          .stream()
          .iterator()
          .asScala
          .flatMap(e => e.getValue.iterator().asScala.map(e.getKey -> _))
          .toVector,
      )

    final implicit def univEq: UnivEq[A] =
      UnivEq.force
  }

}