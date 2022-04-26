package japgolly.webapputil.test

import japgolly.scalajs.react.CallbackTo
import japgolly.webapputil.browser.WindowConfirm

final case class TestWindowConfirm() extends WindowConfirm {
  var nextResponse = true

  private var _calls = 0
  def calls() = _calls

  override def apply(msg: String): CallbackTo[Boolean] =
    CallbackTo {
      _calls += 1
      nextResponse
    }
}

object TestWindowConfirm {
  import TestState._

  class Obs(t: TestWindowConfirm) {
    val calls = t.calls()
  }

  final class TestDsl[R, O, S](val * : Dsl[Id, R, O, S, String])
                              (getRef: R => TestWindowConfirm,
                               getObs: O => Obs) {
    private implicit def autoRef(r: R): TestWindowConfirm = getRef(r)
    private implicit def autoObs(o: O): Obs = getObs(o)

    val calls = *.focus("window.confirm calls").value(_.obs.calls)

    def setNextResponse(r: Boolean): *.Actions =
      *.action("Set next WindowConfirm response to " + r)(_.ref.nextResponse = r)
  }
}
