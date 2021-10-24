package japgolly.webapputil.protocol.entrypoint

import japgolly.univeq.UnivEq

final case class Html(val asString: String) extends AnyVal

object Html {
  implicit def univEq: UnivEq[Html] =
    UnivEq.derive
}
