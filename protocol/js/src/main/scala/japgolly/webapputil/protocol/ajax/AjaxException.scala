package japgolly.webapputil.protocol.ajax

import org.scalajs.dom.XMLHttpRequest

/**
 * Thrown when `Ajax.get` or `Ajax.post` receives a non-20X response code.
 * Contains the XMLHttpRequest that resulted in that response
 *
 * This used to be in scalajs-dom but was deprecated in v2.0.0.
 */
case class AjaxException(xhr: XMLHttpRequest) extends Exception {
  def isTimeout = xhr.status == 0 && xhr.readyState == 4
}
