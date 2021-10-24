package japgolly.webapputil.test

import japgolly.univeq._
import japgolly.webapputil.general.Effect._
import japgolly.webapputil.general.LazyVal
import japgolly.webapputil.http.HttpClient.{Module => _, _}
import scala.runtime.AbstractFunction1
import scala.util.{Failure, Success, Try}

object TestHttpClient {

  class Module[F[_], A[_]](implicit F: Sync[F], A: Async[A]) {
    type Req    = ReqF[F]
    type Client = TestHttpClient[F, A]

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
    val respond   : ResponseDsl[F[Unit]]

    override def toString =
      "Req[%08X]:%s(%s)".format(
        ##,
        request.uri,
        if (request.uriParams.nonEmpty) request.uriParams.toString else request.body.toString
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

  abstract class ResponseDsl[+A] {
    def withResponseAttempt(r: Either[Throwable, Response]): A

    final def apply(body       : String  = "",
                    contentType: String  = null,
                    headers    : Headers = Headers.empty,
                    status     : Status  = Status(200),
                   ): A =
      withResponse(Response(
        status,
        LazyVal.pure(ResponseBody(body, Option(contentType))),
        headers,
      ))

    final def withResponse(r: Response): A =
      withResponseAttempt(Right(r))

    final def withException(err: Throwable = new RuntimeException("Dummy exception from TestHttpClient")): A =
      withResponseAttempt(Left(err))
  }

  var defaultTimeoutMs = 4000
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

  var timeoutMs = -1L

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

  def addAutoResponse(r: Request)(f: Req => F[Unit]): Unit =
    addAutoResponse(_.request ==* r)(f)

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
        val r = onReq(new ReqF[F] { self =>
          override val request = req
          override val onResponse = i => F.delay {
            result = i.fold(Failure(_), Success(_))
            reactNow()
          }
          override val respond =
            new ResponseDsl[F[Unit]] {
              override def withResponseAttempt(r: Either[Throwable, Response]): F[Unit] =
                self.onResponse(r)
            }
        })
        reqs :+= r
        if (autoRespond)
          autoRespondToLast()
      }

      val main =
        A.async[Response] { f =>
          callbacks ::= f
          newReq()
        }

      var timeoutMs = this.timeoutMs
      if (timeoutMs < 0)
        timeoutMs = TestHttpClient.defaultTimeoutMs

      if (timeoutMs <= 0)
        main
      else
        A.timeoutMsOrThrow(timeoutMs, new RuntimeException("TestHttpClient response timed out."))(main)
    }

  def respondToLast: ResponseDsl[Unit] =
    new ResponseDsl[Unit] {
      override def withResponseAttempt(r: Either[Throwable, Response]): Unit = {
        val req = last()
        req.markAsResponded()
        F.runSync(req.onResponse(r))
      }
    }
}
