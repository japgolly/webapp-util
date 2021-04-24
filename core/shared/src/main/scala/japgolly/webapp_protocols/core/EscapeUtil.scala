package japgolly.webapp_protocols.core

trait EscapeUtil {
  def quote(sb: StringBuilder, s: String): Unit
  def quote(s: String): String

  def escape(sb: StringBuilder, s: String): Unit
  def escape(s: String): String
}

object EscapeUtil extends EscapeUtil with EscapeUtil_PlatformSpecific
