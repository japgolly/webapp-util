package japgolly.webapputil.indexeddb

import org.scalajs.dom._
import scala.annotation.elidable
import scala.scalajs.js

final case class IndexedDbError(event: ErrorEvent) extends RuntimeException(
  event.asInstanceOf[js.Dynamic].message.asInstanceOf[js.UndefOr[String]].getOrElse(null)
) {

  // Note: allowing .message to be undefined is presumably only required due to use of fake-indexeddb in tests
  val msg: String =
    event.asInstanceOf[js.Dynamic].message.asInstanceOf[js.UndefOr[String]].getOrElse("")

  @elidable(elidable.FINEST)
  override def toString =
    s"IndexedDb.Error($msg)"

  def isStoredDatabaseHigherThanRequested: Boolean = {
    // Chrome: The requested version (1) is less than the existing version (2).
    // Firefox: The operation failed because the stored database is a higher version than the version requested.
    msg.contains("version") && (msg.contains("higher") || msg.contains("less than"))
  }
}
