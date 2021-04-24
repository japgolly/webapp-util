package japgolly.webapp_protocols.core

// ***********
// *         *
// *   JVM   *
// *         *
// ***********

import java.lang.{StringBuilder => JStringBuilder}

trait EscapeUtil_PlatformSpecific { self: EscapeUtil.type =>

  override def quote(s: String): String = {
    val sb = new JStringBuilder()
    quote(sb, s)
    sb.toString
  }

  override def quote(sb: JStringBuilder, s: String): Unit = {
    sb.append('"')
    escape(sb, s)
    sb.append('"')
  }

  override def escape(s: String): String = {
    val sb = new JStringBuilder()
    escape(sb, s)
    sb.toString
  }

  override def escape(sb: JStringBuilder, s: String): Unit = {
    val chars = s.toCharArray()
    var i = 0
    var c = 'x'
    while (i < chars.length) {
      c = chars(i)
      if (c == '\'')
        sb.append("\\'")
      else if (c == '\"')
        sb.append("\\\"")
      else if (c == '\r')
        sb.append("\\r")
      else if (c == '\n')
        sb.append("\\n")
      else if (c == '\t')
        sb.append("\\t")
      else if (c < 32 || c >= 127)
        sb.append(String.format("\\u%04x", c.toInt))
      else
        sb.append(c)
      i += 1
    }
  }

}