package japgolly.webapputil.boopickle

import japgolly.scalajs.react.callback._
import japgolly.webapputil.binary.{CodecEngine => GenCodecEngine}
import japgolly.webapputil.boopickle.SafePickler.ConstructionHelperImplicits._
import japgolly.webapputil.general._
import japgolly.webapputil.websocket.WebSocketClient.{CloseReasons, OnDecodeError}
import japgolly.webapputil.websocket.WebSocketShared.{CloseReason, ReqId}
import japgolly.webapputil.websocket.{WebSocket, WebSocketClient, WebSocketShared}
import org.scalajs.dom.window

object BoopickleWebSocketClient {

  type Client[ReqRes <: Protocol.RequestResponse[SafePickler]] =
    WebSocketClient[SafePickler, ReqRes]

  type ClientToServer[Req] =
    WebSocketShared.ClientToServer[Req]

  type ServerToClient[Push] =
    WebSocketShared.ServerToClient[SafePickler, Push]

  // ===================================================================================================================

  type CodecEngine = GenCodecEngine[SafePickler, OnDecodeError]

  object CodecEngine {

    def silent: CodecEngine =
      BoopickleCodecEngine.safePickler.mapError[OnDecodeError] { e =>
        CallbackTo {
          val r: CloseReason =
            if (e.isLocalKnownToBeOutOfDate)
              CloseReason.clientOutOfDate
            else
              CloseReasons.parseError
          Some(r)
        }
      }

    def default(logger: LoggerJs): CodecEngine =
      BoopickleCodecEngine.safePickler.mapError[OnDecodeError] { e =>
        CallbackTo {
          logger(_.error(s"Failed to parse server response: $e"))
          val r: CloseReason =
            if (e.isLocalKnownToBeOutOfDate) {
              window.alert("Unable to understand the response from the server.\nWe've upgraded our servers since you opened this page.\nPlease reload this page to get the updates.")
              CloseReason.clientOutOfDate
            } else
              CloseReasons.parseError
          Some(r)
        }
      }
  }

  // ===================================================================================================================

  type Builder[ReqRes <: Protocol.RequestResponse[SafePickler], Push] =
    WebSocketClient.Builder[SafePickler, ReqRes, Push]

  object Builder {

    def apply(urlBase    : Url.Absolute.Base,
              protocol   : Protocol.WebSocket.ClientReqServerPush[SafePickler],
              codecEngine: CodecEngine): Builder[protocol.ReqRes, protocol.Push] = {
      val createWS = CallbackTo(WebSocket(urlBase, protocol))
      apply(createWS, protocol, codecEngine)
    }

    def apply(createWS   : CallbackTo[WebSocket],
              protocol   : Protocol.WebSocket.ClientReqServerPush[SafePickler],
              codecEngine: CodecEngine): Builder[protocol.ReqRes, protocol.Push] =
      WebSocketClient.Builder[SafePickler](protocol)(
        mkProtocolClientServer(_),
        mkProtocolServerClient(_)(_),
        createWS,
        codecEngine,
      )
  }

  // ===================================================================================================================
  // Protocols

  import boopickle.DefaultBasic._

  implicit val picklerReqId: Pickler[ReqId] =
    intPickler.xmap(ReqId.apply)(_.value)

  def mkProtocolClientServer[Req](implicit sp: SafePickler[Req]): Protocol.Of[SafePickler, ClientToServer[Req]] =
    Protocol(sp.map(Tuple2Pickler(picklerReqId, _)))

  /** TAKE CARE! The Protocol.AndValue[SafePickler] part of unpickled results will be null if no unpickler is available!
    */
  def mkProtocolServerClient[Push](responseUnpickler: ReqId => Option[Protocol[SafePickler]])
                                  (implicit pushCodec: SafePickler[Push]): Protocol.Of[SafePickler, ServerToClient[Push]] = {

    val pickler: Pickler[ServerToClient[Push]] =
      new Pickler[ServerToClient[Push]] {

        override def pickle(obj: ServerToClient[Push])(implicit state: PickleState): Unit =
          obj match {
            case Right((i, pv)) =>
              state.enc.writeLong(i.value.toLong << 1)
              pv.codec.embeddedWrite(pv.value)
            case Left(push) =>
              state.enc.writeLong(1)
              pushCodec.embeddedWrite(push)
          }

        override def unpickle(implicit state: UnpickleState): ServerToClient[Push] = {
          val header = state.dec.readLong
          if (header == 1)
            Left(pushCodec.embeddedRead)
          else {
            val reqId = ReqId((header >> 1).toInt)
            val protocol = responseUnpickler(reqId)
            val pav: Protocol.AndValue[SafePickler] =
              protocol match {
                case Some(p) =>
                  val v = p.codec.embeddedRead
                  p.andValue(v)
                case None =>
                  null
              }
            Right((reqId, pav))
          }
        }
      }

    Protocol(pickler.asV1(0))
  }

}
