package japgolly.webapp_protocols.core.general

import japgolly.univeq.UnivEq

final case class ErrorMsg(value: String) {

  // Keep this as a val so that the stack trace points to where the error was created, as opposed to thrown.
  val exception: ErrorMsg.Exception =
    ErrorMsg.Exception(this)

  def throwException(): Nothing =
    throw exception

  def modMsg(f: String => String): ErrorMsg = {
    val e = ErrorMsg(f(value))
    e.exception.setStackTrace(exception.getStackTrace)
    e
  }

  def withPrefix(s: String): ErrorMsg =
    modMsg(s + _)
}

object ErrorMsg {

  implicit def univEq: UnivEq[ErrorMsg] =
    UnivEq.derive

  def fromThrowable(t: Throwable): ErrorMsg =
    apply(Option(t.getMessage).getOrElse(t.toString).trim)

  def errorOccurred(t: Throwable): ErrorMsg =
    Option(t.getMessage).map(_.trim).filter(_.nonEmpty) match {
      case Some(m) => ErrorMsg("Error occurred: " + m)
      case None    => ErrorMsg("Error occurred.")
    }

  object ClientSide {
    def errorContactingServer = ErrorMsg("Error contacting server. Please try again.")
    def noCompatibleServer    = ErrorMsg("Failed to find a compatible server. Please try again, or try reloading the page.")
    def serverCallTimeout     = ErrorMsg("Server didn't respond. Please check your internet connectivity.")
  }

  final case class Exception(msg: ErrorMsg) extends RuntimeException(msg.value)
}
