package japgolly.webapputil.test

import japgolly.scalajs.react._
import japgolly.webapputil.browser.WindowLocation
import japgolly.webapputil.general.Url

final class TestWindowLocation(initial: Url.Absolute) extends WindowLocation {

  var href = initial

  override def setHref(url: Url.Absolute) = Callback {
    href = url
  }

  override def setHrefRelative(url: Url.Relative) = Callback {
    href = href / url
  }
}

object TestWindowLocation {
  def apply(): TestWindowLocation =
    new TestWindowLocation(Url.Absolute("http://localhost"))
}
