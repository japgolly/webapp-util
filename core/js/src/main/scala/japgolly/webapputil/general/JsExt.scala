package japgolly.webapputil.general

import scala.scalajs.js

object JsExt {

  @inline final implicit class JsAnyExt(private val self: Any) extends AnyVal {
    @inline def falsy: Boolean = {
      val a = self.asInstanceOf[js.Dynamic]
      (!a).asInstanceOf[Boolean]
    }

    @inline def truthy: Boolean =
      !falsy
  }
}
