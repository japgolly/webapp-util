package japgolly.webapputil.core.general

trait Protocol[F[_]] { self =>
  type Type
  val codec: F[Type]

  final type AndValue = Protocol.AndValue[F] { type Type = self.Type }

  final def andValue(v: Type): AndValue =
    new Protocol.AndValue[F] {
      override type Type = self.Type
      override val codec = self.codec
      override val value = v
    }
}

object Protocol {

  type Of[F[_], A] = Protocol[F] { type Type = A }

  def apply[F[_], A](c: F[A]): Of[F, A] =
    new Protocol[F] {
      override type Type = A
      override val codec = c
    }

  // ===================================================================================================================

  trait AndValue[F[_]] {
    type Type
    val codec: F[Type]
    val value: Type

    override def toString = s"Protocol.AndValue($value)"

    def unsafeForceType[A]: AndValue.Of[F, A] =
      this.asInstanceOf[AndValue.Of[F, A]]
  }

  object AndValue {
    type Of[F[_], A] = AndValue[F] { type Type = A }
  }

  // ===================================================================================================================

  trait RequestResponse[F[_]] {
    type RequestType
    type ResponseType

    final type PreparedSend = RequestResponse.PreparedSend.Of[F, PreparedRequestType, ResponseType]
    type PreparedRequestType
    def prepareSend(r: RequestType): PreparedSend
  }

  object RequestResponse {

    type Simple[F[_], Req, Res] = RequestResponse[F] {
      type RequestType         = Req
      type ResponseType        = Res
      type PreparedRequestType = Req
    }

    def simple[F[_], Req, Res](res: Protocol.Of[F, Res]): Simple[F, Req, Res] =
      new RequestResponse[F] {
        override type RequestType         = Req
        override type ResponseType        = Res
        override type PreparedRequestType = Req
        override def prepareSend(r: Req) = PreparedSend(r, res)
      }

    // -----------------------------------------------------------------------------------------------------------------
    trait PreparedSend[F[_], Req] {
      val request : Req
      val response: Protocol[F]
    }

    object PreparedSend {
      type Of[F[_], Req, Res] = PreparedSend[F, Req] {
        val request : Req
        val response: Protocol.Of[F, Res]
      }

      def apply[F[_], Req, Res](req: Req, res: Protocol.Of[F, Res]): Of[F, Req, Res] =
        new PreparedSend[F, Req] {
          override val request  = req
          override val response = res
        }
    }
  }

}
