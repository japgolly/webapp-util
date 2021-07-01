package japgolly.webapputil.protocol.test

import japgolly.webapputil.protocol.general.Effect._
import japgolly.webapputil.protocol.general.LazyVal
import japgolly.webapputil.protocol.http.HttpClient.{Module => _, _}
import scala.runtime.AbstractFunction1
import scala.util.{Failure, Success, Try}

object TestHttpClient {

  class Module[F[_], A[_]](implicit F: Sync[F], A: Async[A]) {

    final type Req    = ReqF[F]
    final type Client = TestHttpClient[F, A]

    def apply(autoRespondInitially: Boolean): Client =
      new TestHttpClient(autoRespondInitially)
  }

  object Module {
    def apply[F[_], A[_]](implicit F: Sync[F], A: Async[A]): Module[F, A] =
      new Module
  }

  trait ReqF[F[_]] {
    val request   : Request
    val onResponse: Either[Throwable, Response] => F[Unit]

    override def toString =
      "Req[%08X]:%s(%s)".format(
        ##,
        request.uri,
        if (request.uriParams.nonEmpty) request.uriParams.asVector.toString else request.body.toString
      )

    private var _pendingResponse = true
    final def responsePending = _pendingResponse
    final def responded = !responsePending

    def markAsResponded(): Unit =
      if (responsePending)
        _pendingResponse = false
      else
        throw new java.lang.IllegalStateException("Request has already been responded to.")
  }

  abstract class ResponseDsl {
    def withResponseAttempt(r: Either[Throwable, Response]): Unit

    final def apply(body   : Body    = Body.Empty,
                    headers: Headers = Headers.empty,
                    status : Status  = Status(200),
                   ): Unit =
      withResponse(Response(status, LazyVal.pure(body), headers))

    final def withResponse(r: Response): Unit =
      withResponseAttempt(Right(r))

    final def withException(err: Throwable = new RuntimeException("Dummy exception from TestHttpClient")): Unit =
      withResponseAttempt(Left(err))
  }
}

// =====================================================================================================================

class TestHttpClient[F[_], A[_]](autoRespondInitially: Boolean)
                                (implicit F: Sync[F], A: Async[A]) extends AbstractFunction1[Request, A[Response]] {
  import TestHttpClient._

  type Req = ReqF[F]

  var reqs: Vector[Req] =
    Vector.empty

  def last(): Req =
    reqs.last

  def nthLast(n: Int): Req = {
    val l = reqs.length
    assert(n >= 1, s"n ($n) must be ≥ 1")
    assert(n <= l, s"n ($n) exceeds number of available requests ($l)")
    reqs(l - n)
  }

  def assertReqsSent(expect: Int): Unit = {
    val actual = reqs.length
    if (actual != expect)
      throw new java.lang.AssertionError(s"Expected $expect Http requests but $actual were emited.")
  }

  def clear(): Unit = {
    reqs = Vector.empty
  }

  def reset(): Unit = {
    clear()
    autoRespond = autoRespondInitially
    autoResponsePFs = Nil
    autoResponseFallback = defaultAutoResponseFallback
  }

  var autoRespond: Boolean =
    autoRespondInitially

  var autoResponsePFs: List[PartialFunction[Req, F[Unit]]] =
    Nil

  var autoResponseFallback: Req => F[Unit] =
    defaultAutoResponseFallback

  protected def defaultAutoResponseFallback: Req => F[Unit] =
    r => F.delay(println(s"${Console.YELLOW}Don't know how to respond to $r${Console.RESET}"))

  def addAutoResponsePF(f: PartialFunction[Req, F[Unit]]): Unit =
    autoResponsePFs :+= f

  def addAutoResponse(accept: Req => Boolean)(f: Req => F[Unit]): Unit =
    addAutoResponsePF {
      case req if accept(req) => f(req)
    }

  def autoRespondTo(req: Req): Unit = {
    req.markAsResponded()
    F.runSync(
      autoResponsePFs
        .find(_.isDefinedAt(req))
        .getOrElse(autoResponseFallback)
        .apply(req)
    )
  }

  def autoRespondToLast(): Unit =
    autoRespondTo(last())

  // Override to modify or register each request that passes through
  protected def onReq(req: Req): Req =
    req

  override def apply(request: Request): A[Response] = {
    val far = prepare(request)
    val aar = A.delay(F.runSync(far))
    A.flatten(aar)
  }

  def prepare(req: Request): F[A[Response]] =
    F.delay {

      var callbacks: List[Try[Response] => Unit] =
        Nil

      var result: Try[Response] =
        null

      def reactNow(): Unit = {
        if (result ne null)
          callbacks.foreach(_(result))
      }

      def newReq(): Unit = {
        val r = onReq(new ReqF[F] {
          override val request = req
          override val onResponse = i => F.delay {
            result = i.fold(Failure(_), Success(_))
            reactNow()
          }
        })
        reqs :+= r
        if (autoRespond)
          autoRespondToLast()
      }

      A.async[Response] { f =>
        callbacks ::= f
        newReq()
      }
    }

  def respondToLast: ResponseDsl =
    new ResponseDsl {
      override def withResponseAttempt(r: Either[Throwable, Response]): Unit = {
        val req = last()
        req.markAsResponded()
        F.runSync(req.onResponse(r))
      }
    }
}
