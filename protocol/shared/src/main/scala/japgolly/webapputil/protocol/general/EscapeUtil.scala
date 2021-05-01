package japgolly.webapputil.protocol.general

import java.lang.{StringBuilder => JStringBuilder}

trait EscapeUtil {
  def quote(s: String): String
  def escape(s: String): String

  def appendQuoted(sb: JStringBuilder, s: String): Unit
  def appendEscaped(sb: JStringBuilder, s: String): Unit

  final def appendQuoted(sb: StringBuilder, s: String): Unit = appendQuoted(sb.underlying, s)
  final def appendEscaped(sb: StringBuilder, s: String): Unit = appendEscaped(sb.underlying, s)
}

object EscapeUtil extends EscapeUtil with EscapeUtil_PlatformSpecific
