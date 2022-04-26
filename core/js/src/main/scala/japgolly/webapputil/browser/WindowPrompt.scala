package japgolly.webapputil.browser

import japgolly.scalajs.react.{CallbackTo, Reusability}

/** Abstraction over `window.prompt`. */
trait WindowPrompt {
  def apply(message: String): CallbackTo[Option[String]]
  def apply(message: String, default: String): CallbackTo[Option[String]]
}

object WindowPrompt {

  val real: WindowPrompt =
    new WindowPrompt {
      override def apply(message: String): CallbackTo[Option[String]] =
        CallbackTo.prompt(message)

      override def apply(message: String, default: String): CallbackTo[Option[String]] =
        CallbackTo.prompt(message, default)
    }

  def const(answer: Option[String]): WindowPrompt =
    const(CallbackTo.pure(answer))

  def const(cb: CallbackTo[Option[String]]): WindowPrompt =
    new WindowPrompt {
      override def apply(message: String) = cb
      override def apply(message: String, default: String) = cb
    }

  implicit def reusability: Reusability[WindowPrompt] =
    Reusability.byRef
}