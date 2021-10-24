package japgolly.webapputil.general

/** Equivalent to a `Map[String, List[String]]`. */
final class MultiStringMap(asVector: Vector[(String, String)], isNormalised: Boolean)
    extends AbstractMultiStringMap[MultiStringMap](asVector, isNormalised) {

  override protected def create(asVector: Vector[(String, String)], isNormalised: Boolean = false) =
    new MultiStringMap(asVector, isNormalised)

  override def toMultiStringMap: MultiStringMap =
    this
}

object MultiStringMap extends AbstractMultiStringMap.Module[MultiStringMap] {

  override def fromVector(v: Vector[(String, String)]): MultiStringMap =
    new MultiStringMap(v, isNormalised = false)
}
