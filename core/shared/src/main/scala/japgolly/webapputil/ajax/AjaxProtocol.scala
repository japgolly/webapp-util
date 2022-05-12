package japgolly.webapputil.ajax

import japgolly.webapputil.general.{Protocol, Url}

object AjaxProtocol {

  /** Simple because the response protocol is static and independent of request data.
    * This is opposed to dependently-typed protocols where the response protocol depends on the request value.
    *
    * Note: the `F[_]` type parameter is the message encoder (eg. `JsonCodec`, `boopickle.Pickler`)
    */
  final case class Simple[F[_], _Req, _Res](url: Url.Relative,
                                            req: Protocol.Of[F, _Req],
                                            res: Protocol.Of[F, _Res]) extends AjaxProtocol[F] {
    type Req = _Req
    type Res = _Res
    override val protocol: Protocol.RequestResponse.Simple[F, Req, Res] = Protocol.RequestResponse.simple(res)
    override val requestProtocol = req
    override def responseProtocol(req: Req) = res
  }
}

  /** Note: the `F[_]` type parameter is the message encoder (eg. `JsonCodec`, `boopickle.Pickler`)
    */
trait AjaxProtocol[F[_]] {
  val url            : Url.Relative
  val protocol       : Protocol.RequestResponse[F]
  val requestProtocol: Protocol.Of[F, protocol.PreparedRequestType]

  def responseProtocol(req: protocol.PreparedRequestType): Protocol.Of[F, protocol.ResponseType]

  final type ServerSideFn  [G[_]      ] =     protocol.PreparedRequestType  => G[ protocol.ResponseType]
  final type ServerSideFnI [G[_], I   ] = (I, protocol.PreparedRequestType) => G[ protocol.ResponseType]
  final type ServerSideFnO [G[_],    O] =     protocol.PreparedRequestType  => G[(protocol.ResponseType, O)]
  final type ServerSideFnIO[G[_], I, O] = (I, protocol.PreparedRequestType) => G[(protocol.ResponseType, O)]
}
