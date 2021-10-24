package japgolly.webapputil.ajax

import japgolly.webapputil.general.{Protocol, Url}

object AjaxProtocol {
  final case class Simple[F[_], _Req, _Res](url: Url.Relative,
                                            req: Protocol.Of[F, _Req],
                                            res: Protocol.Of[F, _Res]) extends AjaxProtocol[F] {
    type Req = _Req
    type Res = _Res
    override val protocol: Protocol.RequestResponse.Simple[F, Req, Res] = Protocol.RequestResponse.simple(res)
    override val prepReq = req
    override def responseProtocol(req: Req) = res
  }
}

trait AjaxProtocol[F[_]] {
  val url     : Url.Relative
  val protocol: Protocol.RequestResponse[F]
  val prepReq : Protocol.Of[F, protocol.PreparedRequestType]

  def responseProtocol(req: protocol.PreparedRequestType): Protocol.Of[F, protocol.ResponseType]

  final type ServerSideFn  [G[_]      ] =     protocol.PreparedRequestType  => G[ protocol.ResponseType]
  final type ServerSideFnI [G[_], I   ] = (I, protocol.PreparedRequestType) => G[ protocol.ResponseType]
  final type ServerSideFnO [G[_],    O] =     protocol.PreparedRequestType  => G[(protocol.ResponseType, O)]
  final type ServerSideFnIO[G[_], I, O] = (I, protocol.PreparedRequestType) => G[(protocol.ResponseType, O)]
}
