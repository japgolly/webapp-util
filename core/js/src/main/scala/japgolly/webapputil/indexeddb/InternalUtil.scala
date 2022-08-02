package japgolly.webapputil.indexeddb

import japgolly.scalajs.react._
import org.scalajs.dom._
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

private[indexeddb] object InternalUtil {

  val success_ = Success(())

  def asyncRequest_[R <: IDBRequest[Any, _]](act: => R): AsyncCallback[Unit] =
    asyncRequest(act)(_ => ())

  def asyncRequest[R <: IDBRequest[Any, _], A](act: => R)(onSuccess: R => A): AsyncCallback[A] =
    AsyncCallback.promise[A].asAsyncCallback.flatMap { case (promise, complete) =>
      val raw = act

      raw.onerror = event => {
        complete(Failure(IndexedDbError(event))).runNow()
      }

      raw.onsuccess = _ => {
        complete(Try(onSuccess(raw))).runNow()
      }

      promise
    }

  def mkStoreArray(stores: Seq[ObjectStoreDef[_, _]]): js.Array[String] = {
    val a = new js.Array[String]
    stores.foreach(s => a.push(s.name))
    a
  }

  def dispatchRequest[R <: IDBRequest[Any, _], A](raw: R)(onSuccess: R => A): Unit = {
    raw.onerror = event => {
      complete(Failure(IndexedDbError(event))).runNow()
    }

    raw.onsuccess = _ => {
      complete(Try(onSuccess(raw))).runNow()
    }
  }
    AsyncCallback.promise[A].asAsyncCallback.flatMap { case (promise, complete) =>
      val raw = act


      promise
    }

}
