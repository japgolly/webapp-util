package japgolly.webapp_protocols.core.entrypoint

import japgolly.webapp_protocols.core.EscapeUtil

object EntrypointInvoker {
  final case class JS(val asString: String) extends AnyVal {
    // def asXml = <script type="text/javascript">{invokeOnLoadJs(i)}</script>
  }
}

final class EntrypointInvoker[Input](defn: EntrypointDef[Input]) {
  import EntrypointInvoker.JS

  private val runCmdHead =
    defn.objectAndMethod + "('"

  private val appendEncoded: (StringBuilder, String) => Unit =
    if (defn.codec.escapeEncodedString)
      EscapeUtil.escape
    else
      _.append(_)

  private def call(sb: StringBuilder, i: Input): Unit = {
    sb.append(runCmdHead)
    appendEncoded(sb, defn.codec.encode(i))
    sb.append("')")
  }

  def apply(i: Input): JS = {
    // TODO Potential optimisation: have this estimate a good initial SB size for itself by observing past results
    val sb = new StringBuilder(256)
    call(sb, i)
    JS(sb.toString)
  }

  def onLoad(i: Input): JS = {
    // TODO Potential optimisation: have this estimate a good initial SB size for itself by observing past results
    val sb = new StringBuilder(256)
    sb.append("window.onload=function(){")
    call(sb, i)
    sb.append("};") // why'd I add a semi-colon here again? Can't remember...
    JS(sb.toString)
  }
}
