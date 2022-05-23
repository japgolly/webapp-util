package japgolly.webapputil.ajax

import japgolly.scalajs.react.AsyncCallback
import japgolly.scalajs.react.extra.Ajax
import japgolly.webapputil.binary.BinaryData
import japgolly.webapputil.general._
import org.scalajs.dom.XMLHttpRequest
import scala.scalajs.js.typedarray.ArrayBuffer

trait AjaxClient[P[_]] {
  def asyncFunction(p: AjaxProtocol[P]): AsyncFunction[
                                           p.protocol.RequestType,
                                           ErrorMsg,
                                           p.protocol.ResponseType]
}

object AjaxClient {

  /** Calls are never made. AsyncCallbacks never complete. */
  def never[P[_]]: AjaxClient[P] =
    new AjaxClient[P] {
      override def asyncFunction(p: AjaxProtocol[P]) =
        AsyncFunction.const(AsyncCallback.never[Either[ErrorMsg, p.protocol.ResponseType]])
    }

  trait Response[+A] {
    def shouldRetry: Boolean
    def result: Either[ErrorMsg, A]
  }

  object Response {

    def apply[A](result: Either[ErrorMsg, A]): Response[A] =
      apply(result, shouldRetry = result.isLeft)

    def apply[A](result: Either[ErrorMsg, A], shouldRetry: Boolean): Response[A] = {
      val _shouldRetry = shouldRetry
      val _result      = result
      new Response[A] {
        override def shouldRetry = _shouldRetry
        override def result      = _result
      }
    }

    def success[A](result: A): Response[A] =
      apply(Right(result))
  }

  trait WithRetries[P[_]] extends AjaxClient[P] {

    protected def singleCall(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Response[p.protocol.ResponseType]]

    protected val maxRetries: Int =
      2

    protected def callWithRetry(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Response[p.protocol.ResponseType]] = {
      val once = singleCall(p)(req)
      AsyncCallback.tailrec(maxRetries) { retriesRemaining =>
        if (retriesRemaining > 0)
          once.attempt.flatMap {
            case Right(r)               if r.shouldRetry   => AsyncCallback.pure(Left(retriesRemaining - 1))
            case Right(r)                                  => AsyncCallback.pure(Right(r))
            case Left(AjaxException(x)) if x.status == 501 => AsyncCallback.pure(Left(retriesRemaining - 1)) // server rejected due to protocol ver diff
            case Left(e)                                   => AsyncCallback.throwException(e)
          }
        else
          once.map(Right(_))
      }
    }

    override def asyncFunction(p: AjaxProtocol[P]): AsyncFunction[p.protocol.RequestType, ErrorMsg, p.protocol.ResponseType] =
      AsyncFunction
        .simple((req: p.protocol.RequestType) => callWithRetry(p)(req).map(_.result))
        .extractErrorFromOutput
  }

  trait Binary[P[_]] extends WithRetries[P] {
    protected def encode[A](p: P[A], a: A): BinaryData
    protected def decode[A](p: P[A], b: BinaryData): Response[A]

    protected def isSuccess(xhr: XMLHttpRequest): Boolean =
      xhr.status >= 200 && xhr.status < 300

    override protected def singleCall(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Response[p.protocol.ResponseType]] = {
      val prep   = p.protocol.prepareSend(req)
      val reqBin = encode(p.requestProtocol.codec, prep.request)

      Ajax("POST", p.url.relativeUrl)
        .setRequestHeader("Content-Type", "application/octet-stream")
        .and(_.responseType = "arraybuffer")
        .send(reqBin.unsafeArrayBuffer)
        .asAsyncCallback
        .map { xhr =>
          if (isSuccess(xhr)) {
            val ab       = xhr.response.asInstanceOf[ArrayBuffer]
            val resBin   = BinaryData.unsafeFromArrayBuffer(ab)
            val resCodec = prep.response.codec
            decode(resCodec, resBin)
          } else
            throw AjaxException(xhr)
        }
    }
  }

  trait Json[P[_]] extends WithRetries[P] {
    protected def encode[A](p: P[A], a: A): String
    protected def decode[A](p: P[A], j: String): Response[A]

    protected def isSuccess(xhr: XMLHttpRequest): Boolean =
      (xhr.status >= 200 && xhr.status < 300) && (xhr.getResponseHeader("Content-Type") match {
        case null => true
        case t    => t.takeWhile(_ != ';') == "application/json"
      })

    override def singleCall(p: AjaxProtocol[P])(req: p.protocol.RequestType): AsyncCallback[Response[p.protocol.ResponseType]] = {
      val prep    = p.protocol.prepareSend(req)
      val reqJson = encode(p.requestProtocol.codec, prep.request)

      Ajax("POST", p.url.relativeUrl)
        .setRequestContentTypeJsonUtf8
        .send(reqJson)
        .asAsyncCallback
        .map { xhr =>
          if (isSuccess(xhr)) {
            val resJson  = xhr.responseText
            val resCodec = prep.response.codec
            decode(resCodec, resJson)
          } else
            throw AjaxException(xhr)
        }
    }
  }

}