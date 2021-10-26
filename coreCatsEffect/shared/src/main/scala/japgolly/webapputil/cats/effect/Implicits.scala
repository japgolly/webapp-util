package japgolly.webapputil.cats.effect

object Implicits extends Implicits

trait Implicits
  extends PlatformImplicits
     with WebappUtilEffectIO.Implicits
