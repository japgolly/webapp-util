package japgolly.webapputil.indexeddb

import cats.kernel.Eq
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Util.{identity => identityFn}
import japgolly.univeq.UnivEqCats._
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

  // ===================================================================================================================
  // Main types and classes

  import Internals._

  final case class DatabaseName(value: String)

  type OpenResult = OpenCallbacks => AsyncCallback[Database]

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

  final case class VersionChange(db: DatabaseInVersionChange, oldVersion: Int, newVersion: Option[Int]) {
    def createObjectStore[K, V](ver: Int, defn: ObjectStoreDef[K, V]): Callback =
      db.createObjectStore(defn).when_(oldVersion < ver && newVersion.exists(_ >= ver))
  }

  final class DatabaseInVersionChange(raw: IDBDatabase) {
    def createObjectStore[K, V](defn: ObjectStoreDef[K, V]): Callback =
      Callback {
        raw.createObjectStore(defn.name)
      }
  }

  final class Database(raw: IDBDatabase, onClose: Callback) {

    def close: Callback = {
      val actuallyClose =
        Callback(raw.close()).attempt

      // The onclose event handler is only fired "when the database is unexpectedly closed".
      // Therefore we call it here explicitly.
      // https://developer.mozilla.org/en-US/docs/Web/API/IDBDatabase/onclose
      actuallyClose >> onClose
    }

    def compareAndSet(stores: ObjectStoreDef[_, _]*): CasDsl1 =
      new CasDsl1(this, stores)

    /** Performs an async modification on a store value.
      *
      * This only modifies an existing value. Use [[modifyAsyncOption()]] to upsert and/or delete values.
      *
      * This uses [[compareAndSet()]] for atomicity and thread-safety.
      *
      * @return If the value exists, this returns the previous and updated values
      */
    def modifyAsync[K, V](store: ObjectStoreDef.Async[K, V])(key: K)(f: V => AsyncCallback[V]): AsyncCallback[Option[(V, V)]] =
      compareAndSet(store)
        .getValueAsync(store)(key)
        .mapAsync(AsyncCallback.traverseOption(_)(v1 => f(v1).map((v1, _))))
        .putResultWhenDefinedBy(store)(key, _.map(_._2))

    /** Performs an async modification on an optional store value.
      *
      * This uses [[compareAndSet()]] for atomicity and thread-safety.
      *
      * @return The previous and updated values
      */
    def modifyAsyncOption[K, V](store: ObjectStoreDef.Async[K, V])(key: K)(f: Option[V] => AsyncCallback[Option[V]]): AsyncCallback[(Option[V], Option[V])] =
      compareAndSet(store)
        .getValueAsync(store)(key)
        .mapAsync { o1 => f(o1).map((o1, _)) }
        .putResultWhenDefinedBy(store)(key, _._2)

    def transactionRO: RunTxnDsl1[RO] =
      new RunTxnDsl1(raw, TxnDslRO, IDBTransactionMode.readonly)

    def transactionRW: RunTxnDsl1[RW] =
      new RunTxnDsl1(raw, TxnDslRW, IDBTransactionMode.readwrite)

    // Convenience methods

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(add(store.sync)(key, _))

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store).flatMap(_.add(key, value)))

    /** Note: insert only */
    def addWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      AsyncCallback.traverseOption_(value)(add(store)(key, _))

    /** Note: insert only */
    def addWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      AsyncCallback.traverseOption_(value)(add(store)(key, _))

    def clear[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store.sync).flatMap(_.clear))

    def delete[K, V](store: ObjectStoreDef[K, V])(key: K): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store.sync).flatMap(_.delete(key)))

    def get[K, V](store: ObjectStoreDef.Async[K, V])(key: K): AsyncCallback[Option[V]] =
      get(store.sync)(key).flatMap(AsyncCallback.traverseOption(_)(_.decode))

    def get[K, V](store: ObjectStoreDef.Sync[K, V])(key: K): AsyncCallback[Option[V]] =
      transactionRO(store)(_.objectStore(store).flatMap(_.get(key)))

    def getAllKeys[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Vector[K]] =
      transactionRO(store)(_.objectStore(store.sync).flatMap(_.getAllKeys))

    def getAllValues[K, V](store: ObjectStoreDef.Async[K, V]): AsyncCallback[Vector[V]] =
      getAllValues(store.sync).flatMap(AsyncCallback.traverse(_)(_.decode))

    def getAllValues[K, V](store: ObjectStoreDef.Sync[K, V]): AsyncCallback[Vector[V]] =
      transactionRO(store)(_.objectStore(store).flatMap(_.getAllValues))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(put(store.sync)(key, _))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      transactionRW(store)(_.objectStore(store).flatMap(_.put(key, value)))

    /** aka upsert */
    def putWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      AsyncCallback.traverseOption_(value)(put(store)(key, _))

    /** aka upsert */
    def putWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      AsyncCallback.traverseOption_(value)(put(store)(key, _))
  } // class Database

  final class ObjectStore[K, V](val defn: ObjectStoreDef.Sync[K, V]) {
    import defn.{keyCodec, valueCodec}

    private implicit def autoWrap[M <: TxnMode, A](s: TxnStep[M, A]): Txn[M, A] =
      Txn(s)

    /** Note: insert only */
    def add(key: K, value: V): Txn[RW, Unit] = {
      val k = keyCodec.encode(key)
      TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StoreAdd(this, k, _))
    }

    /** Note: insert only */
    def addWhenDefined(key: K, value: Option[V]): Txn[RW, Unit] =
      value.fold[Txn[RW, Unit]](TxnStep.unit)(add(key, _))

    def clear: Txn[RW, Unit] =
      TxnStep.StoreClear(this)

    def delete(key: K): Txn[RW, Unit] =
      TxnStep.StoreDelete(this, keyCodec.encode(key))

    def get(key: K): Txn[RO, Option[V]] =
      TxnStep.StoreGet(this, keyCodec.encode(key))

    def getAllKeys: Txn[RO, Vector[K]] =
      TxnStep.StoreGetAllKeys(this)

    def getAllValues: Txn[RO, Vector[V]] =
      TxnStep.StoreGetAllVals(this)

    /** aka upsert */
    def put(key: K, value: V): Txn[RW, Unit] = {
      val k = keyCodec.encode(key)
      TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StorePut(this, k, _))
    }

    /** aka upsert */
    def putWhenDefined(key: K, value: Option[V]): Txn[RW, Unit] =
      value.fold[Txn[RW, Unit]](TxnStep.unit)(put(key, _))
  }

  // ===================================================================================================================
  // DSLs

  final class RunTxnDsl1[M <: TxnMode] private[IndexedDb](raw: IDBDatabase, txnDsl: TxnDsl[M], mode: IDBTransactionMode) {
    def apply(stores: ObjectStoreDef[_, _]*): RunTxnDsl2[M] =
      new RunTxnDsl2(raw, txnDsl, mode, mkStoreArray(stores))
  }

  final class RunTxnDsl2[M <: TxnMode] private[IndexedDb](raw: IDBDatabase, txnDsl: TxnDsl[M], mode: IDBTransactionMode, stores: js.Array[String]) {

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
  }

  final class CasDsl1(db: Database, stores: Seq[ObjectStoreDef[_, _]]) {

    def get[A](f: TxnDsl[RO] => Txn[RO, A])(implicit e: Eq[A]) =
      getAndCompareBy(f)(e.eqv)

    def getAndCompareBy_==[A](f: TxnDsl[RO] => Txn[RO, A]) =
      getAndCompareBy(f)(_ == _)

    def getAndCompareBy[A](f: TxnDsl[RO] => Txn[RO, A])(eql: (A, A) => Boolean) =
      new CasDsl2[A](db, stores, f(TxnDslRO), eql)

    /** Note: CAS comparison is on the raw IDB value, i.e. the result prior to async decoding */
    def getValueAsync[K, V](store: ObjectStoreDef.Async[K, V])(key: K): CasDsl3[Option[store.Value], Option[V]] =
      get(_.objectStore(store).flatMap(_.get(key)))
        .mapAsync(AsyncCallback.traverseOption(_)(_.decode))

    /** Note: CAS comparison is on `Option[V]`, i.e. the decoded result */
    def getValueSync[K, V](store: ObjectStoreDef.Sync[K, V])(key: K)(implicit e: Eq[Option[V]]): CasDsl2[Option[V]] =
      get(_.objectStore(store).flatMap(_.get(key)))

    def getAllKeys[K, V](store: ObjectStoreDef[K, V])(implicit e: Eq[Vector[K]]): CasDsl2[Vector[K]] =
      get(_.objectStore(store.sync).flatMap(_.getAllKeys))

    def getAllValuesAsync[K, V](store: ObjectStoreDef.Async[K, V]): CasDsl3[Vector[store.Value], Vector[V]] =
      get(_.objectStore(store).flatMap(_.getAllValues))
        .mapAsync(AsyncCallback.traverse(_)(_.decode))

    def getAllValuesSync[K, V](store: ObjectStoreDef.Sync[K, V])(implicit e: Eq[Vector[V]]): CasDsl2[Vector[V]] =
      get(_.objectStore(store.sync).flatMap(_.getAllValues))
  }

  sealed trait CasDsl23[A, B] {
    def mapAsync[C](f: B => AsyncCallback[C]): CasDsl3[A, C]

    final def map[C](f: B => C) =
      mapAsync(b => AsyncCallback.pure(f(b)))

    final def mapSync[C](f: B => CallbackTo[C]) =
      mapAsync(f(_).asAsyncCallback)
  }

  final class CasDsl2[A](db: Database, stores: Seq[ObjectStoreDef[_, _]], get: Txn[RO, A], eql: (A, A) => Boolean) extends CasDsl23[A, A] {

    private def next = mapAsync(AsyncCallback.pure)

    override def mapAsync[C](f: A => AsyncCallback[C]) =
      new CasDsl3[A, C](db, stores, get, eql, f)

    def set[C](set: TxnDsl[RW] => A => Txn[RW, C]): AsyncCallback[C] =
      next.set { dsl => (a, _) => set(dsl)(a) }
  }

  final class CasDsl3[A, B](db: Database, stores: Seq[ObjectStoreDef[_, _]], get: Txn[RO, A], eql: (A, A) => Boolean, prep: A => AsyncCallback[B]) extends CasDsl23[A, B] {

    override def mapAsync[C](f: B => AsyncCallback[C]) =
      new CasDsl3[A, C](db, stores, get, eql, prep(_).flatMap(f))

    def set[C](set: TxnDsl[RW] => (A, B) => Txn[RW, C]): AsyncCallback[C] = {
      val txnRO = db.transactionRO(stores: _*)
      val txnRW = db.transactionRW(stores: _*)

      def loopTxn(a: A, b: B): AsyncCallback[Either[A, C]] =
        txnRW { dsl =>
          (get: Txn[RW, A]).flatMap { a2 =>
            if (eql(a, a2))
              set(dsl)(a, b).map(Right(_))
            else
              dsl.pure(Left(a2))
          }
        }

      def loop(a: A): AsyncCallback[Either[A, C]] =
        for {
          b <- prep(a)
          e <- loopTxn(a, b)
        } yield e

      txnRO(_ => get).flatMap(AsyncCallback.tailrec(_)(loop))
    }

    // Convenience methods

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(add(store.sync)(key, _))

    /** Note: insert only */
    def add[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      setConst(_.objectStore(store).flatMap(_.add(key, value)))

    /** Note: insert only */
    def addWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      value.fold(AsyncCallback.unit)(add(store)(key, _))

    /** Note: insert only */
    def addWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      value.fold(AsyncCallback.unit)(add(store)(key, _))

    /** Note: insert only */
    def addResult[K](store: ObjectStoreDef.Async[K, B])(key: K): AsyncCallback[B] =
      addResultBy(store)(key, identityFn)

    /** Note: insert only */
    def addResult[K](store: ObjectStoreDef.Sync[K, B])(key: K): AsyncCallback[B] =
      addResultBy(store)(key, identityFn)

    /** Note: insert only */
    def addResultBy[K, V](store: ObjectStoreDef.Async[K, V])(key: K, f: B => V): AsyncCallback[B] =
      mapAsync { b => store.encode(f(b)).map((b, _)) }
      .addResultBy(store.sync)(key, _._2)
      .map(_._1)

    /** Note: insert only */
    def addResultBy[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, f: B => V): AsyncCallback[B] =
      set(dsl => (_, b) =>
        for {
          s <- dsl.objectStore(store)
          _ <- s.add(key, f(b))
        } yield b
      )

    /** Note: insert only */
    @inline def addResultWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
      addResultWhenDefinedBy(store)(key, ev)

    /** Note: insert only */
    @inline def addResultWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
      addResultWhenDefinedBy(store)(key, ev)

    /** Note: insert only */
    def addResultWhenDefinedBy[K, V](store: ObjectStoreDef.Async[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
      mapAsync { b => AsyncCallback.traverseOption(f(b))(store.encode(_)).map((b, _)) }
      .addResultWhenDefinedBy(store.sync)(key, _._2)
      .map(_._1)

    /** Note: insert only */
    def addResultWhenDefinedBy[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
      set(dsl => (_, b) =>
        f(b) match {
          case Some(v) =>
            for {
              s <- dsl.objectStore(store)
              _ <- s.add(key, v)
            } yield b
          case None =>
            dsl.pure(b)
        }
      )

    def clear[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Unit] =
      setConst(_.objectStore(store.sync).flatMap(_.clear))

    def delete[K, V](store: ObjectStoreDef[K, V])(key: K): AsyncCallback[Unit] =
      setConst(_.objectStore(store.sync).flatMap(_.delete(key)))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
      store.encode(value).flatMap(put(store.sync)(key, _))

    /** aka upsert */
    def put[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
      setConst(_.objectStore(store).flatMap(_.put(key, value)))

    /** Note: upsert */
    def putWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      value.fold(AsyncCallback.unit)(put(store)(key, _))

    /** Note: upsert */
    def putWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
      value.fold(AsyncCallback.unit)(put(store)(key, _))

    /** Note: upsert */
    def putResult[K](store: ObjectStoreDef.Async[K, B])(key: K): AsyncCallback[B] =
      putResultBy(store)(key, identityFn)

    /** Note: upsert */
    def putResult[K](store: ObjectStoreDef.Sync[K, B])(key: K): AsyncCallback[B] =
      putResultBy(store)(key, identityFn)

    /** Note: upsert */
    def putResultBy[K, V](store: ObjectStoreDef.Async[K, V])(key: K, f: B => V): AsyncCallback[B] =
      mapAsync { b =>
        val v = f(b)
        for {
          enc <- store.encode(v)
        } yield (b, enc)
      }
      .putResultBy(store.sync)(key, _._2)
      .map(_._1)

    /** Note: upsert */
    def putResultBy[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, f: B => V): AsyncCallback[B] =
      set(dsl => (_, b) =>
        for {
          s <- dsl.objectStore(store)
          _ <- s.put(key, f(b))
        } yield b
      )

    /** Note: upsert */
    @inline def putResultWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
      putResultWhenDefinedBy(store)(key, ev)

    /** Note: upsert */
    @inline def putResultWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
      putResultWhenDefinedBy(store)(key, ev)

    /** Note: upsert */
    def putResultWhenDefinedBy[K, V](store: ObjectStoreDef.Async[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
      mapAsync { b => AsyncCallback.traverseOption(f(b))(store.encode(_)).map((b, _)) }
      .putResultWhenDefinedBy(store.sync)(key, _._2)
      .map(_._1)

    /** Note: upsert */
    def putResultWhenDefinedBy[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
      set(dsl => (_, b) =>
        f(b) match {
          case Some(v) =>
            for {
              s <- dsl.objectStore(store)
              _ <- s.put(key, v)
            } yield b
          case None =>
            dsl.pure(b)
        }
      )

    def setConst[C](set: TxnDsl[RW] => Txn[RW, C]): AsyncCallback[C] =
      this.set(dsl => (_, _) => set(dsl))
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
