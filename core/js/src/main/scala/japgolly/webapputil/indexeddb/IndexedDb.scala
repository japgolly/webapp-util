package japgolly.webapputil.indexeddb

import japgolly.scalajs.react._
import japgolly.webapputil.indexeddb.TxnMode._
import org.scalajs.dom._
import scala.annotation.elidable
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

final class IndexedDb(raw: IDBFactory) {
  import IndexedDb._
  import IndexedDb.Internals._

  def open(name: DatabaseName): OpenResult =
    _open(raw.open(name.value))

  def open(name: DatabaseName, version: Int): OpenResult =
    _open(raw.open(name.value, version))

  private def _open(rawOpen: => IDBOpenDBRequest[IDBDatabase]): OpenResult =
    callbacks => {

      def create(): IDBOpenDBRequest[IDBDatabase] = {
        val r = rawOpen

        // r.onblocked = callbacks.blocked.toJsFn1

        r.onupgradeneeded = e => {
          val db = new DatabaseInVersionChange(r.result)
          val args = versionChange(db, e)
          callbacks.upgradeNeeded(args).runNow()
        }

        r
      }

      asyncRequest(create()) { r =>
        val rawDb = r.result

        rawDb.onversionchange = e => {
          try {
            val args = versionChange(new DatabaseInVersionChange(rawDb), e)
            callbacks.versionChange(args).runNow()
          } finally {
            // We close the DB at the end of this event on matter what so that other connections don't block and we
            // don't have to handle onblocked events.
            rawDb.close()

            // The onclose event handler is only fired "when the database is unexpectedly closed".
            // Therefore we call it here explicitly.
            // https://developer.mozilla.org/en-US/docs/Web/API/IDBDatabase/onclose
            callbacks.closed.runNow()
          }
        }

        rawDb.onclose = _ => {
          callbacks.closed.runNow()
        }

        new Database(rawDb, onClose = callbacks.closed)
      }
    }

  def deleteDatabase(name: DatabaseName): AsyncCallback[Unit] =
    asyncRequest_(raw.deleteDatabase(name.value))
}

// =====================================================================================================================

object IndexedDb {

  def apply(raw: IDBFactory): IndexedDb =
    new IndexedDb(raw)

  def global(): Option[IndexedDb] =
    try {
      window.indexedDB.toOption.map(apply)
    } catch {
      case _: Throwable => None
    }

  final case class DatabaseName(value: String)

  type OpenResult = OpenCallbacks => AsyncCallback[Database]

  import Internals._

  final case class VersionChange(db: DatabaseInVersionChange, oldVersion: Int, newVersion: Option[Int]) {
    def createObjectStore[K, V](ver: Int, defn: ObjectStoreDef[K, V]): Callback =
      db.createObjectStore(defn).when_(oldVersion < ver && newVersion.exists(_ >= ver))
  }

  /** Callbacks to install when opening a DB.
   *
   * Note 1: On `versionChange`, the DB connection will be closed automatically.
   *
   * Note 2: There's no `blocked` handler because we currently don't allow blocking. To quote the idb spec:
   *         if "there are open connections that don’t close in response to a versionchange event, the request will be
   *         blocked until all they close".
   */
  final case class OpenCallbacks(upgradeNeeded: VersionChange => Callback,
                                 versionChange: VersionChange => Callback,
                                 closed       : Callback)

  final case class Error(event: ErrorEvent) extends RuntimeException(
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

  final class DatabaseInVersionChange(raw: IDBDatabase) {
    def createObjectStore[K, V](defn: ObjectStoreDef[K, V]): Callback =
      Callback {
        raw.createObjectStore(defn.name)
      }
  }

  // ===================================================================================================================

  final class Database(raw: IDBDatabase, onClose: Callback) {

    def close: Callback = {
      val actuallyClose =
        Callback(raw.close()).attempt

      // The onclose event handler is only fired "when the database is unexpectedly closed".
      // Therefore we call it here explicitly.
      // https://developer.mozilla.org/en-US/docs/Web/API/IDBDatabase/onclose
      actuallyClose >> onClose
    }

    def transactionRO: TxnStep1[RO] =
      new TxnStep1(TxnDslRO, IDBTransactionMode.readonly)

    def transactionRW: TxnStep1[RW] =
      new TxnStep1(TxnDslRW, IDBTransactionMode.readwrite)

    final class TxnStep1[M <: TxnMode] private[Database](txnDsl: TxnDsl[M], mode: IDBTransactionMode) {
      def apply(stores: ObjectStoreDef[_, _]*): TxnStep2[M] =
        new TxnStep2(txnDsl, mode, mkStoreArray(stores))
    }

    final class TxnStep2[M <: TxnMode] private[Database](txnDsl: TxnDsl[M], mode: IDBTransactionMode, stores: js.Array[String]) {

      def apply[A](f: TxnDsl[M] => Txn[M, A]): AsyncCallback[A] = {
        val x = CallbackTo.pure(f(txnDsl))
        sync(_ => x)
      }

      def sync[A](dslCB: TxnDsl[M] => CallbackTo[Txn[M, A]]): AsyncCallback[A] = {

        @inline def startRawTxn(complete: Try[Unit] => Callback) = {
          val txn = raw.transaction(stores, mode)

          txn.onerror = event => {
            complete(Failure(Error(event))).runNow()
          }

          txn.oncomplete = complete(success_).toJsFn1

          txn
        }

        for {
          dsl <- dslCB(txnDsl).asAsyncCallback

          (awaitTxnCompletion, complete) <- AsyncCallback.promise[Unit].asAsyncCallback

          result <- AsyncCallback.suspend {
            val txn = startRawTxn(complete)
            interpretTxn(txn, dsl)
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

    } // TxnStep2

    // Convenience methods

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store).flatMap(_.add(key, value)))

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(add(store.sync)(key, _))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store).flatMap(_.put(key, value)))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(put(store.sync)(key, _))

    def get[K, V](store: ObjectStoreDef.Sync[K, V])(key: K): AsyncCallback[Option[V]] =
      transactionRO(store)(_.objectStore(store).flatMap(_.get(key)))

    def get[K, V](store: ObjectStoreDef.Async[K, V])(key: K): AsyncCallback[Option[V]] =
      get(store.sync)(key).flatMap(AsyncCallback.traverseOption(_)(_.decode))

    def getAllKeys[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Vector[K]] =
      transactionRO(store)(_.objectStore(store.sync).flatMap(_.getAllKeys))

    def getAllValues[K, V](store: ObjectStoreDef.Sync[K, V]): AsyncCallback[Vector[V]] =
      transactionRO(store)(_.objectStore(store).flatMap(_.getAllValues))

    def getAllValues[K, V](store: ObjectStoreDef.Async[K, V]): AsyncCallback[Vector[V]] =
      getAllValues(store.sync).flatMap(AsyncCallback.traverse(_)(_.decode))

    def delete[K, V](store: ObjectStoreDef[K, V])(key: K): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store.sync).flatMap(_.delete(key)))

    def clear[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store.sync).flatMap(_.clear))

  } // class Database

  // ===================================================================================================================

  final class ObjectStore[K, V](val defn: ObjectStoreDef.Sync[K, V]) {
    import defn.{keyCodec, valueCodec}

    private implicit def autoWrap[M <: TxnMode, A](s: TxnStep[M, A]): Txn[M, A] =
      Txn(s)

    /** Note: insert only */
    def add(key: K, value: V): Txn[RW, Unit] = {
      val k = keyCodec.encode(key)
      TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StoreAdd(this, k, _))
    }

    /** aka upsert */
    def put(key: K, value: V): Txn[RW, Unit] = {
      val k = keyCodec.encode(key)
      TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StorePut(this, k, _))
    }

    def get(key: K): Txn[RO, Option[V]] =
      TxnStep.StoreGet(this, keyCodec.encode(key))

    def getAllKeys: Txn[RO, Vector[K]] =
      TxnStep.StoreGetAllKeys(this)

    def getAllValues: Txn[RO, Vector[V]] =
      TxnStep.StoreGetAllVals(this)

    def delete(key: K): Txn[RW, Unit] =
      TxnStep.StoreDelete(this, keyCodec.encode(key))

    def clear: Txn[RW, Unit] =
      TxnStep.StoreClear(this)
  }

  // ===================================================================================================================
  private object Internals {

    val success_ = Success(())

    def asyncRequest_[R <: IDBRequest[Any, _]](act: => R): AsyncCallback[Unit] =
      asyncRequest(act)(_ => ())

    def asyncRequest[R <: IDBRequest[Any, _], A](act: => R)(onSuccess: R => A): AsyncCallback[A] =
      AsyncCallback.promise[A].asAsyncCallback.flatMap { case (promise, complete) =>
        val raw = act

        raw.onerror = event => {
          complete(Failure(Error(event))).runNow()
        }

        raw.onsuccess = _ => {
          complete(Try(onSuccess(raw))).runNow()
        }

        promise
      }

    def versionChange(db: DatabaseInVersionChange, e: IDBVersionChangeEvent): VersionChange =
      VersionChange(db, e.oldVersion.toInt, e.newVersionOption.map(_.toInt))

    def mkStoreArray(stores: Seq[ObjectStoreDef[_, _]]): js.Array[String] = {
      val a = new js.Array[String]
      stores.foreach(s => a.push(s.name))
      a
    }

    def interpretTxn[M <: TxnMode, A](txn: IDBTransaction, dsl: Txn[M, A]): AsyncCallback[A] =
      AsyncCallback.suspend {
        import TxnStep._

        val stores = js.Dynamic.literal().asInstanceOf[js.Dictionary[IDBObjectStore]]

        def getStore(s: ObjectStore[_, _]) =
          AsyncCallback.delay(stores.get(s.defn.name).get)

        def interpret[B](step: TxnStep[TxnMode, B]): AsyncCallback[B] = {
          step match {

            case FlatMap(fa, f) =>
              interpret(fa).flatMap(a => interpret(f(a)))

            case StoreGet(s, k) =>
              getStore(s).flatMap { store =>
                asyncRequest(store.get(k.asJs))(_.result).flatMapSync { result =>
                  if (js.isUndefined(result))
                    CallbackTo.pure(None)
                  else
                    s.defn.valueCodec.decode(result).map(Some(_))
                }
              }

            case Eval(c) =>
              c.asAsyncCallback

            case Suspend(s) =>
              s.asAsyncCallback.flatMap(interpret(_))

            case GetStore(sd) =>
              AsyncCallback.delay {
                val s = txn.objectStore(sd.name)
                stores.put(sd.name, s)
                new ObjectStore(sd)
              }

            case StoreAdd(s, k, v) =>
              getStore(s).flatMap { store =>
                asyncRequest_(store.add(v, k.asJs))
              }

            case StorePut(s, k, v) =>
              getStore(s).flatMap { store =>
                asyncRequest_(store.put(v, k.asJs))
              }

            case Map(fa, f) =>
              interpret(fa).map(f)

            case StoreDelete(s, k) =>
              getStore(s).flatMap { store =>
                asyncRequest_(store.delete(k.asJs))
              }

            case StoreGetAllKeys(s) =>
              import s.defn.keyCodec
              getStore(s).flatMap { store =>
                asyncRequest(store.getAllKeys()) { req =>
                  val rawKeys = req.result
                  Vector.tabulate(rawKeys.length) { i =>
                    val rawKey = rawKeys(i)
                    keyCodec.decode(IndexedDbKey.fromJs(rawKey)).runNow() // safe in asyncRequest onSuccess
                  }
                }
              }

            case StoreGetAllVals(s) =>
              import s.defn.valueCodec
              getStore(s).flatMap { store =>
                asyncRequest(store.getAll()) { req =>
                  val rawVals = req.result
                  Vector.tabulate(rawVals.length) { i =>
                    val rawVal = rawVals(i)
                    valueCodec.decode(rawVal).runNow() // safe in asyncRequest onSuccess
                  }
                }
              }

            case StoreClear(s) =>
              getStore(s).flatMap { store =>
                asyncRequest_(store.clear())
              }

            case TailRec(z, f) =>
              AsyncCallback.tailrec(z)(a => interpret(f(a)))

          }
        }

        interpret(dsl.step)
      }

  } // object Internals
}
