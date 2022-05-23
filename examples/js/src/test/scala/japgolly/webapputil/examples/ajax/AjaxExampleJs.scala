package japgolly.webapputil.examples.ajax

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.webapputil.ajax.AjaxClient
import japgolly.webapputil.circe.JsonAjaxClient
import japgolly.webapputil.general.{AsyncFunction, ErrorMsg}

object AjaxExampleJs {
  import AjaxExampleShared.AddInts.{protocol, Request, Response}

  // There are different ways to make AJAX calls depending on your app and needs.

  // ===================================================================================
  // This is a primative example that makes a single call attempt and nothing else

  def primativeExample(): AsyncCallback[AjaxClient.Response[Response]] =
    JsonAjaxClient.singleCall(protocol)(Request(3, 8))

  // ===================================================================================
  // This is a higher-level example.
  //   - has automatic retries built-in by default
  //   - successful results are wrapped in Either[ErrorMsg, _]
  //   - out-of-protocol errors (eg. client lost connectivity) are caught and converted
  //     into a Left[ErrorMsg].
  //     (see AsyncFunction.throwableToErrorMsg for details)

  val asyncFunction: AsyncFunction[Request, ErrorMsg, Response] =
    JsonAjaxClient.asyncFunction(protocol)

  def betterExample(): AsyncCallback[Either[ErrorMsg, Response]] =
    asyncFunction(Request(3, 8))

  // ===================================================================================
  // Here is an example of making AJAX calls from a React component.

  object ExampleComponent {

    // Notice how we just ask for a AsyncFunction here, which is effectively a
    //   Request => AsyncCallback[Either[ErrorMsg, Response]]
    //
    // The component doesn't care about the details of how call is made, which makes it
    // super easy to test. In fact, it needn't even be an AJAX call! It could be a
    // websocket call, a webworker call, etc. Whoever uses the component gets to decide,
    // no changes here are necessary.
    //
    final case class Props(addInts: AsyncFunction[Request, ErrorMsg, Response])

    sealed trait State
    object State {
      case object Ready extends State
      case object Pending extends State
      final case class Finished(result: Either[ErrorMsg, Response]) extends State
    }

    final class Backend($: BackendScope[Props, State]) {

      // Hard-coding the request for simplicity
      private val req = Request(2, 2)

      private val onSubmit =
        for {
          _      <- $.setStateAsync(State.Pending)
          p      <- $.props.asAsyncCallback
          result <- p.addInts(req) // no need to catch errors here,
                                   // as per the AsyncFunction contract
          _      <- $.setStateAsync(State.Finished(result))
        } yield ()

      private def submitButton =
        <.button(
          s"What really is ${req.m} + ${req.n}? Ask our proprietary Addition Service!",
          ^.onClick --> onSubmit)

      def render(s: State): VdomNode =
        s match {
          case State.Ready                => submitButton
          case State.Pending              => submitButton(^.disabled := true)
          case State.Finished(Right(res)) => <.div(s"${req.m} + ${req.n} = $res! OMG!")
          case State.Finished(Left(err))  => <.div(^.color.red, "Error: ", err.value)
        }
    }

    val Component = ScalaComponent.builder[Props]
      .initialState[State](State.Ready)
      .renderBackend[Backend]
      .build
  }

  // ===================================================================================
  // Here is another example of making AJAX calls from a React component, this time in a
  // way that makes lower-level testing a bit easier.
  //
  // Most of this is the same as above, except for where there are comments.

  object ExampleComponent2 {

    // Notice we request a JsonAjaxClient and not a AsyncFunction here.
    // The JsonAjaxClient could be a real one that makes real calls, or an in-memory
    // instance for lower-level testing.
    final case class Props(ajaxClient: JsonAjaxClient)

    sealed trait State
    object State {
      case object Ready extends State
      case object Pending extends State
      final case class Finished(result: Either[ErrorMsg, Response]) extends State
    }

    final class Backend($: BackendScope[Props, State]) {

      private val req = Request(2, 2)

      private val onSubmit =
        for {
          _      <- $.setStateAsync(State.Pending)
          p      <- $.props.asAsyncCallback
          fn      = p.ajaxClient.asyncFunction(protocol) // create a means to make the
                                                         // AJAX call
          result <- fn(req) // again: no need to catch errors here, as per the
                            // AsyncFunction contract
          _      <- $.setStateAsync(State.Finished(result))
        } yield ()

      private def submitButton =
        <.button(
          s"What really is ${req.m} + ${req.n}? Ask our proprietary Addition Service!",
          ^.onClick --> onSubmit)

      def render(s: State): VdomNode =
        s match {
          case State.Ready                => submitButton
          case State.Pending              => submitButton(^.disabled := true)
          case State.Finished(Right(res)) => <.div(s"${req.m} + ${req.n} = $res! Wow!")
          case State.Finished(Left(err))  => <.div(^.color.red, "Error: ", err.value)
        }
    }

    val Component = ScalaComponent.builder[Props]
      .initialState[State](State.Ready)
      .renderBackend[Backend]
      .build
  }
}
