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

  @inline final implicit class JsArrayExt[A](private val self: js.Array[A]) extends AnyVal {
    def forEachJs(f: js.Function1[A, Unit]): Unit = {
      self.asInstanceOf[js.Dynamic].forEach(f)
      ()
    }
  }

}
