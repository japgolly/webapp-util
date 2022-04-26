package japgolly.webapputil.browser

import japgolly.scalajs.react._
import japgolly.webapputil.general.Url
import org.scalajs.dom.window

trait WindowLocation {
  def setHref        (url: Url.Absolute): Callback
  def setHrefRelative(url: Url.Relative): Callback
}

object WindowLocation {

  object Real extends WindowLocation {
    private[this] def set(href: String) = Callback {
      window.location.href = href
    }

    override def setHref        (url: Url.Absolute) = set(url.absoluteUrl)
    override def setHrefRelative(url: Url.Relative) = set(url.relativeUrl)
  }
}
