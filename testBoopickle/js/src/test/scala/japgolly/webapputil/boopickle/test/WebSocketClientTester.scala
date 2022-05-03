package japgolly.webapputil.boopickle.test

import boopickle.DefaultBasic._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.scalajs.react._
import japgolly.webapputil.boopickle.SafePickler.ConstructionHelperImplicits._
import japgolly.webapputil.boopickle._
import japgolly.webapputil.general._
import japgolly.webapputil.test.{TestTimersJs, TestWebSocket}
import japgolly.webapputil.websocket.WebSocket.ReadyState
import japgolly.webapputil.websocket.WebSocketShared.ReqId
import japgolly.webapputil.websocket._
import japgolly.webapputil.webstorage.AbstractWebStorage
import java.time.Duration
import scala.annotation.nowarn
import sourcecode.Line
import utest._

object WebSocketClientTester {

  def apply(): WebSocketClientTester =
    new WebSocketClientTester

  final case class ReqMsg(msg: Int)
  final case class ResMsg(msg: Int)
  final case class PushMsg(msg: Int)

  implicit def univEq: UnivEq[PushMsg] = UnivEq.derive

  object P extends Protocol.WebSocket.ClientReqServerPush[SafePickler] {
    override type ReqId  = WebSocketShared.ReqId
    // override type ReqRes = Protocol.RequestResponse.Simple[SafePickler, ReqMsg, ResMsg]
    override type ReqRes = Protocol.RequestResponse.Simple[SafePickler, req.Type, ResMsg] // Scala 3 bug
    override val url     = Url.Relative("/x")
    override val req     = Protocol(transformPickler(ReqMsg.apply)(_.msg).asV1(0))
    override val push    = Protocol(transformPickler(PushMsg.apply)(_.msg).asV1(0))
    val res              = Protocol(transformPickler(ResMsg.apply)(_.msg).asV1(0))
    val ReqRes: ReqRes   = Protocol.RequestResponse.simple(res)
  }

  val serverProtocols = WebSocketTestUtil.Protocols(P)

  final val debug = false

  def debugPrintln(s: => Any): Unit =
    if (debug) println("             " + s)
}

@nowarn("msg=scala3bug")
class WebSocketClientTester {
  import WebSocketClientTester._
  import WebSocketClient.State

  // Avoid Scala 3 bug
  private implicit def scala3bug1(p: P.Push): PushMsg = p.asInstanceOf[PushMsg]
  private implicit def scala3bug2(p: PushMsg): P.Push = p.asInstanceOf[P.Push]
  private implicit def scala3bug3(p: P.Req): ReqMsg = p.asInstanceOf[ReqMsg]
  private implicit def scala3bug4(p: ReqMsg): P.Req = p.asInstanceOf[P.Req]

  var webSockets = Vector.empty[TestWebSocket]
  var stateChanges = Vector.empty[State]
  var sentPushes = Vector.empty[PushMsg]
  var receivedPushes = Vector.empty[PushMsg]
  var sendResults = Vector.empty[Vector[Either[Throwable, Either[ErrorMsg, ResMsg]]]]
  var reauthAttempts = 0

  var nextReauthResult: () => Permission =
    () => Deny

  var nextServerPushMsg: () => PushMsg = {
    var prev = 0
    () => {
      prev += 1
      PushMsg(prev)
    }
  }

  val retries = Retries.exponentially(Duration.ofMillis(10))

  val timers = TestTimersJs()

  val newWS = CallbackTo {
    val f = new TestWebSocket("fake url", ReadyState.Connecting)
    f.onSend.unsafeSet(_ => ())
    webSockets :+= f
    f
  }

  def onStateChange(s: State): Callback =
    Callback {
      stateChanges :+= s
      debugPrintln(s"State change: $s")
    }

  val localStorage =
    AbstractWebStorage.inMemory()

  val expiredKey =
    AbstractWebStorage.Key("expired")

  val sessionExpiry: VarJs[Boolean] =
    VarJs.webStorage.boolean(localStorage, expiredKey)

  val codecEngine =
    BoopickleWebSocketClient.CodecEngine.silent

  val client = BoopickleWebSocketClient.Builder(newWS, P, codecEngine)
    .build(
      reauthorise   = AsyncCallback.delay { reauthAttempts += 1; nextReauthResult() },
      onServerPush  = (p: P.Push) => Callback { receivedPushes :+= p },
      onStateChange = (_: Any) => onStateChange,
      retries       = retries,
      sessionExpiry = sessionExpiry,
      timers        = timers,
      logger        = LoggerJs.on) // don't turn this off - it being on, has caught bugs before

  val invoker = client.invoker(P.ReqRes)

  def sendMsg(): Unit = {
    val reqId = sendResults.length
    sendResults :+= Vector.empty
    debugPrintln(s"Sending ReqMsg($reqId)")
    invoker(ReqMsg(reqId)).attempt.map { r =>
      debugPrintln(s"Received response for $reqId: $r")
      sendResults = sendResults.updated(reqId, sendResults(reqId) :+ r)
    }.toCallback.runNow()
    ()
  }

  def ws(): TestWebSocket =
    webSockets.lastOption.getOrElse(sys.error("webSockets is empty"))

  def latestMsg() = webSockets.reverseIterator.flatMap(_.sent().reverseIterator).next()

  object server {
    def parseRequest(msg: TestWebSocket.Message = latestMsg()) =
      serverProtocols.protocolCS.codec.decode(msg.binaryData).getOrThrow()

    def respondBy(f: ReqMsg => ResMsg) = {
      val (reqId, reqMsg) = parseRequest()
      respond(reqId, f(reqMsg))
    }

    def respond(reqId: ReqId, resMsg: ResMsg) = {
      val res = Right((reqId, P.res.andValue(resMsg)))
      val bd = serverProtocols.protocolSC.codec.encode(res)
      ws().recv(TestWebSocket.Message.ArrayBuffer(bd.toArrayBuffer))
    }

    def respondToNextPending(): Unit =
      ws().respondToNextPending { sent =>
        debugPrintln(s"Server received ${sent.binaryData}")
        val (reqId, reqMsg) = serverProtocols.protocolCS.codec.decode(sent.binaryData).getOrThrow()
        val resMsg = ResMsg(reqMsg.msg * -1)
        debugPrintln(s"Responding to $reqId $reqMsg with $resMsg")
        val res = Right((reqId, P.res.andValue(resMsg)))
        val bd = serverProtocols.protocolSC.codec.encode(res)
        TestWebSocket.Message.ArrayBuffer(bd.toArrayBuffer)
      }

    def push(msg: PushMsg = nextServerPushMsg()): Unit = {
      val bin = serverProtocols.protocolSC.codec.encode(Left(msg))
      ws().recv(TestWebSocket.Message.ArrayBuffer(bin.toArrayBuffer))
      sentPushes :+= msg
    }
  }

  def send(req: ReqMsg): AsyncCallback[ResMsg] =
    client.send(P.ReqRes)(req).runNow()

  def assertAndClearStateChanges(expect: State*)(implicit l: Line): Unit = {
    assertEq(stateChanges, expect.toVector)
    stateChanges = Vector.empty
  }

  def checkInvariants(): Unit = {
    assertEq(receivedPushes, sentPushes)
    for (r <- sendResults)
      assert(r.length <= 1)
  }

}
