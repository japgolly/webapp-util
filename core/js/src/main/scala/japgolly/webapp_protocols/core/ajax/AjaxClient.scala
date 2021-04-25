package japgolly.webapp_protocols.core.ajax

import japgolly.scalajs.react.AsyncCallback
import japgolly.webapp_protocols.core.general._

trait AjaxClient[P[_]] {
  def invoker(p: AjaxProtocol[P]): ServerSideProcInvoker[
                                     p.protocol.RequestType,
                                     ErrorMsg,
                                     p.protocol.ResponseType]
}

object AjaxClient {

  /** Calls are never made. AsyncCallbacks never complete. */
  def never[P[_]]: AjaxClient[P] =
    new AjaxClient[P] {
      override def invoker(p: AjaxProtocol[P]) =
        ServerSideProcInvoker.const(AsyncCallback.never[Either[ErrorMsg, p.protocol.ResponseType]])
    }

}