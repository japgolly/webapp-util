package japgolly.webapp_protocols.core

// **********
// *        *
// *   JS   *
// *        *
// **********

import java.lang.{StringBuilder => JStringBuilder}

trait EscapeUtil_PlatformSpecific { self: EscapeUtil.type =>

  override def quote(s: String): String =
    scala.scalajs.js.JSON.stringify(s)

  override def appendQuoted(sb: JStringBuilder, s: String): Unit =
    sb.append(quote(s))

  override def escape(s: String): String =
    if (s == null)
      null
    else {
      val q = quote(s)
      q.substring(1, q.length - 1)
    }

  override def appendEscaped(sb: JStringBuilder, s: String): Unit =
    sb.append(escape(s))

}
