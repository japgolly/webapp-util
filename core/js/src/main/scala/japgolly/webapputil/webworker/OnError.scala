package japgolly.webapputil.webworker

import japgolly.scalajs.react.Callback
import japgolly.webapputil.general.ErrorMsg
import org.scalajs.dom.console

final case class OnError(handle: ErrorMsg => Callback) extends AnyVal {
  @inline def apply(e: ErrorMsg) = handle(e)
}

object OnError {
  def logToConsole: OnError =
    OnError(err => Callback(console.error(err)))
}
