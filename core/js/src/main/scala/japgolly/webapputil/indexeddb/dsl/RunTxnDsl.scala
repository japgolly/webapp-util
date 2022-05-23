package japgolly.webapputil.indexeddb.dsl

import japgolly.scalajs.react._
import japgolly.webapputil.indexeddb.InternalUtil._
import japgolly.webapputil.indexeddb._
import org.scalajs.dom._
import scala.scalajs.js
import scala.util.{Failure, Try}

final class RunTxnDsl1[M <: TxnMode] (raw: IDBDatabase, txnDsl: TxnDsl[M], mode: IDBTransactionMode) {
  def apply(stores: ObjectStoreDef[_, _]*): RunTxnDsl2[M] =
    new RunTxnDsl2(raw, txnDsl, mode, mkStoreArray(stores))
}

final class RunTxnDsl2[M <: TxnMode] (raw: IDBDatabase, txnDsl: TxnDsl[M], mode: IDBTransactionMode, stores: js.Array[String]) {

  def apply[A](f: TxnDsl[M] => Txn[M, A]): AsyncCallback[A] = {
    val x = CallbackTo.pure(f(txnDsl))
    sync(_ => x)
  }

  def sync[A](dslCB: TxnDsl[M] => CallbackTo[Txn[M, A]]): AsyncCallback[A] = {

    @inline def startRawTxn(complete: Try[Unit] => Callback) = {
      val txn = raw.transaction(stores, mode)

      txn.onerror = event => {
        complete(Failure(IndexedDbError(event))).runNow()
      }

      txn.oncomplete = complete(success_).toJsFn1

      txn
    }

    for {
      dsl <- dslCB(txnDsl).asAsyncCallback

      (awaitTxnCompletion, complete) <- AsyncCallback.promise[Unit].asAsyncCallback

      result <- AsyncCallback.suspend {
        val txn = startRawTxn(complete)
        TxnStep.interpretTxn(txn, dsl)
      }

      _ <- awaitTxnCompletion

    } yield result
  }

  def async[A](dsl: TxnDsl[M] => AsyncCallback[Txn[M, A]]): AsyncCallback[A] =
    // Note: This is safer than it looks.
    //       1) This is `Dsl => AsyncCallback[Txn[A]]`
    //          and not `Dsl => Txn[AsyncCallback[A]]`
    //       2) Everything within Txn is still synchronous and lawful
    //       3) Only one transaction is created (i.e. only one call to `apply`)
    dsl(txnDsl).flatMap(txnA => apply(_ => txnA))
}
