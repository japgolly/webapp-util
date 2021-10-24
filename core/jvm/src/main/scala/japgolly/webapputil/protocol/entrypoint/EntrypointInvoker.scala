package japgolly.webapputil.protocol.entrypoint

import japgolly.webapputil.protocol.general.EscapeUtil
import java.lang.{StringBuilder => JStringBuilder}

object EntrypointInvoker {

  def apply[I](defn: EntrypointDef[I]): EntrypointInvoker[I] =
    new EntrypointInvoker(defn)

  // TODO Potential optimisation: have this estimate a good initial SB size for itself by observing past results
  private[EntrypointInvoker] final val ExpectedJsLength = 256
}

final class EntrypointInvoker[Input](defn: EntrypointDef[Input]) {
  import EntrypointInvoker.ExpectedJsLength

  private val runCmdHead =
    defn.objectAndMethod + "(\""

  private val appendEncoded: (JStringBuilder, String) => Unit =
    if (defn.codec.escapeEncodedString)
      EscapeUtil.appendEscaped
    else
      _.append(_)

  private def call(sb: JStringBuilder, i: Input): Unit = {
    sb.append(runCmdHead)
    appendEncoded(sb, defn.codec.encode(i))
    sb.append("\")")
  }

  def apply(i: Input): Js = {
    val sb = new JStringBuilder(ExpectedJsLength)
    call(sb, i)
    Js(sb.toString)
  }

  def apply(w: Js.Wrapper, i: Input): Js = {
    val sb = new JStringBuilder(ExpectedJsLength + w.totalLength)
    sb.append(w.before)
    call(sb, i)
    sb.append(w.after)
    Js(sb.toString)
  }
}
