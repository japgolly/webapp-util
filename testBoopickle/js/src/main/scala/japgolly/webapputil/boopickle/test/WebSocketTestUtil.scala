package japgolly.webapputil.boopickle.test

import japgolly.webapputil.boopickle._
import japgolly.webapputil.general._
import japgolly.webapputil.websocket.WebSocketShared.{ClientToServer, ReqId, ServerToClient}

object WebSocketTestUtil {

  final class Protocols[Req, Push](val protocolCS: Protocol.Of[SafePickler, ClientToServer[Req]],
                                   val protocolSC: Protocol.Of[SafePickler, ServerToClient[SafePickler, Push]])

  object Protocols {

    def apply(p: Protocol.WebSocket.ClientReqServerPush[SafePickler]): Protocols[p.Req, p.Push] = {
      implicit def picklerReq: SafePickler[p.Req] = p.req.codec
      implicit def picklerPush: SafePickler[p.Push] = p.push.codec

      new Protocols[p.Req, p.Push](
        BoopickleWebSocketClient.mkProtocolClientServer,
        BoopickleWebSocketClient.mkProtocolServerClient(responseUnpickler))
    }

    def responseUnpickler[Codec[_]]: ReqId => Option[Protocol[Codec]] =
      _ => ErrorMsg("Server doesn't unpickle responses.").throwException()
  }

}
