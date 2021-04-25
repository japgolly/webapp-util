package japgolly.webapp_protocols.core.ajax

import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import japgolly.webapp_protocols.core.binary.BinaryData
import japgolly.webapp_protocols.core.general._
import org.scalajs.dom.ext.AjaxException
import org.scalajs.dom.raw.XMLHttpRequest
import scala.scalajs.js.typedarray.ArrayBuffer

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

  trait Binary[P[_]] {
    def encode[A](p: P[A], a: A): BinaryData
    def decode[A](p: P[A], b: BinaryData): Result[A]

    trait Result[A] {
      def allowRetry: Boolean
      def result: Either[ErrorMsg, A]
    }

    val isSuccess: XMLHttpRequest => Boolean =
      _.status == 200

    val maxRetries: Int =
      2

    final lazy val ajaxClient: AjaxClient[P] = new AjaxClient[P] {

      def runOnce(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Result[p.protocol.ResponseType]] = {
        val prep   = p.protocol.prepareSend(req)
        val reqBin = encode(p.prepReq.codec, prep.request)

        Ajax("POST", p.url.relativeUrl)
          .setRequestHeader("Content-Type", "application/octet-stream")
          .and(_.responseType = "arraybuffer")
          .send(reqBin.unsafeArrayBuffer)
          .asAsyncCallback
          .map { xhr =>
            if (isSuccess(xhr)) {
              val ab       = xhr.response.asInstanceOf[ArrayBuffer]
              val resCodec = prep.response.codec
              val bin      = BinaryData.unsafeFromArrayBuffer(ab)
              decode(resCodec, bin)
            } else
              throw AjaxException(xhr)
          }
      }

      def runWithRetry(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Result[p.protocol.ResponseType]] = {
        val once = runOnce(p)(req)
        AsyncCallback.tailrec(maxRetries) { retriesRemaining =>
          if (retriesRemaining > 0)
            once.attempt.flatMap {
              case Right(r)               if r.allowRetry    => AsyncCallback.pure(Left(retriesRemaining - 1))
              case Right(r)                                  => AsyncCallback.pure(Right(r))
              case Left(AjaxException(x)) if x.status == 501 => AsyncCallback.pure(Left(retriesRemaining - 1)) // server rejected due to protocol ver diff
              case Left(e)                                   => AsyncCallback.throwException(e)
            }
          else
            once.map(Right(_))
        }
      }

      override def invoker(p: AjaxProtocol[P]): ServerSideProcInvoker[p.protocol.RequestType, ErrorMsg, p.protocol.ResponseType] =
        ServerSideProcInvoker
          .fromSimple((req: p.protocol.RequestType) => CallbackTo(runWithRetry(p)(req).map(_.result)))
          .mergeFailure
    }
  }
}