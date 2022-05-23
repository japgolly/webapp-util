package japgolly.webapputil.test

import japgolly.scalajs.react.{AsyncCallback, Callback, CallbackTo}
import japgolly.webapputil.ajax._
import japgolly.webapputil.general._
import org.scalajs.dom.console
import scala.annotation.nowarn
import scala.util.{Failure, Success, Try}

object TestAjaxClient {

  trait Module {
    type Codec[A]
    type Req                             = TestAjaxClient.Req
    type ReqOf[P <: AjaxProtocol[Codec]] = TestAjaxClient.ReqOf[Codec, P]
    type Client                          = TestAjaxClient[Codec]

    def apply(autoRespondInitially: Boolean): Client =
      new TestAjaxClient(autoRespondInitially)
  }

  object Module {
    type ForCodec[F[_]] = Module { type Codec[A] = F[A] }

    def apply[F[_]]: ForCodec[F] =
      new Module { override type Codec[A] = F[A] }
  }

  type ReqOf[F[_], P <: AjaxProtocol[F]] = Req {type Codec[A] = F[A]; val ajax: P}

  trait Req {
    type Codec[A]
    type Response[A] = AjaxClient.Response[A]
    val ajax      : AjaxProtocol[Codec]
    val input     : ajax.protocol.RequestType
    val onResponse: Either[Throwable, Response[ajax.protocol.ResponseType]] => Callback
    val response  : TestAjaxClient.ResponseDsl[ajax.protocol.ResponseType, Callback]

    override def toString =
      "Req[%08X]:%s(%s)".format(##, ajax.url, input)

    def asResponseTo(p: AjaxProtocol[Codec]): ReqOf[Codec, p.type] = {
      assert(ajax eq p)
      this.asInstanceOf[ReqOf[Codec, p.type]]
    }

    private var _pendingResponse = true
    final def responsePending = _pendingResponse
    final def responded = !responsePending

    def markAsResponded(): Unit =
      if (responsePending)
        _pendingResponse = false
      else
        throw new java.lang.IllegalStateException("Request has already been responded to.")
  }

  abstract class ResponseDsl[-A, +B] {

    final def apply(value: A): B =
      withResponse(AjaxClient.Response.success(value))

    final def withResponse(r: AjaxClient.Response[A]): B =
      withResponseAttempt(Right(r))

    final def withException(err: Throwable = new RuntimeException("Dummy exception from TestAjaxClient")): B =
      withResponseAttempt(Left(err))

    def withResponseAttempt(r: Either[Throwable, AjaxClient.Response[A]]): B
  }

  var defaultTimeoutMs = 4000
}

class TestAjaxClient[F[_]](autoRespondInitially: Boolean) extends AjaxClient[F] {

  final type Req                         = TestAjaxClient.Req {type Codec[A] = F[A]}
  final type ReqOf[P <: AjaxProtocol[F]] = TestAjaxClient.ReqOf[F, P]

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
      throw new java.lang.AssertionError(s"Expected $expect AJAX requests but $actual were emited.")
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

  var autoResponsePFs: List[PartialFunction[Req, Callback]] =
    Nil

  protected def defaultAutoResponseFallback: Req => Callback =
    r => Callback(console.warn(s"${Console.YELLOW}Don't know how to respond to $r${Console.RESET}"))

  var autoResponseFallback: Req => Callback =
    defaultAutoResponseFallback

  def addAutoResponsePF(f: PartialFunction[Req, Callback]): Unit =
    autoResponsePFs :+= f

  def addAutoResponse(p: AjaxProtocol[F])(f: ReqOf[p.type] => Callback): Unit =
    addAutoResponsePF {
      case r if r.ajax eq p => f(r.asResponseTo(p))
    }

  def autoRespondTo(req: Req): Unit = {
    req.markAsResponded()
    autoResponsePFs
      .find(_.isDefinedAt(req))
      .getOrElse(autoResponseFallback)
      .apply(req)
      .runNow()
  }

  def autoRespondToLast(): Unit =
    autoRespondTo(last())

  @nowarn("cat=unused")
  protected def processsResponse(p  : AjaxProtocol[F])
                                (req: p.protocol.RequestType,
                                 res: AjaxClient.Response[p.protocol.ResponseType]): Either[ErrorMsg, p.protocol.ResponseType] =
    res.result

  override def asyncFunction(p: AjaxProtocol[F]): AsyncFunction[p.protocol.RequestType, ErrorMsg, p.protocol.ResponseType] =
    AsyncFunction.fromSimple { (req: p.protocol.RequestType) =>
      apply(p)(req).map(_.map(processsResponse(p)(req, _)))
    }.mergeFailure

  protected def onReq(req: Req): Req =
    req

  def apply(p: AjaxProtocol[F])(req: p.protocol.RequestType) = CallbackTo[AsyncCallback[AjaxClient.Response[p.protocol.ResponseType]]] {
    type Resp = AjaxClient.Response[p.protocol.ResponseType]

    var callbacks: List[Try[Resp] => Callback] =
      Nil

    var result: Try[Resp] =
      null

    def reactNow(): Unit = {
      if (result ne null)
        callbacks.foreach(_(result).runNow())
    }

    def newReq(): Unit = {
      val r = onReq(new TestAjaxClient.Req { self =>
        override type Codec[A] = F[A]
        override val ajax: p.type = p
        override val input = req
        override val onResponse = i => Callback {
          result = i.fold(Failure(_), Success(_))
          reactNow()
        }
        override val response =
          new TestAjaxClient.ResponseDsl[p.protocol.ResponseType, Callback] {
            override def withResponseAttempt(r: Either[Throwable,AjaxClient.Response[p.protocol.ResponseType]]) =
              self.onResponse(r)
          }
      })
      reqs :+= r
      if (autoRespond)
        autoRespondToLast()
    }

    val main =
      AsyncCallback[Resp](f =>
        Callback {
          callbacks ::= f
          newReq()
        }
      )

    var timeoutMs = this.timeoutMs
    if (timeoutMs < 0)
      timeoutMs = TestAjaxClient.defaultTimeoutMs

    if (timeoutMs <= 0)
      main
    else
      main
        .timeoutMs(timeoutMs.toDouble)
        .map(_.getOrElse(throw new RuntimeException("TestAjaxClient response timed out.")))
  }

  def respondToLast(p: AjaxProtocol[F]): TestAjaxClient.ResponseDsl[p.protocol.ResponseType, Unit] =
    new TestAjaxClient.ResponseDsl[p.protocol.ResponseType, Unit] {
      override def withResponseAttempt(r: Either[Throwable,AjaxClient.Response[p.protocol.ResponseType]]): Unit = {
        val req = last().asResponseTo(p)
        req.markAsResponded()
        req.onResponse(r).runNow()
      }
    }
}
