package japgolly.webapp_protocols.core

import java.lang.{StringBuilder => JStringBuilder}

trait EscapeUtil {
  def quote(sb: JStringBuilder, s: String): Unit
  def quote(s: String): String

  def escape(sb: JStringBuilder, s: String): Unit
  def escape(s: String): String
}

object EscapeUtil extends EscapeUtil with EscapeUtil_PlatformSpecific
