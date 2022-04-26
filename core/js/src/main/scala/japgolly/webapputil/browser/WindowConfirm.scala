package japgolly.webapputil.browser

import japgolly.scalajs.react.{CallbackTo, Reusability}

/** Abstraction over `window.confirm`. */
trait WindowConfirm {
  def apply(msg: String): CallbackTo[Boolean]
}

object WindowConfirm {

  val real: WindowConfirm =
    CallbackTo.confirm

  def const(b: Boolean): WindowConfirm =
    const(CallbackTo.pure(b))

  def const(cb: CallbackTo[Boolean]): WindowConfirm =
    _ => cb

  implicit def reusability: Reusability[WindowConfirm] =
    Reusability.byRef
}