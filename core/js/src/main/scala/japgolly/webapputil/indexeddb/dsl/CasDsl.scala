package japgolly.webapputil.indexeddb.dsl

import cats.kernel.Eq
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Util.{identity => identityFn}
import japgolly.univeq.UnivEqCats._
import japgolly.webapputil.indexeddb.TxnMode._
import japgolly.webapputil.indexeddb._

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

  /** Note: insert only */
  def addWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value.fold(AsyncCallback.unit)(add(store)(key, _))

  /** Note: insert only */
  def addWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value.fold(AsyncCallback.unit)(add(store)(key, _))

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

  /** aka upsert or delete */
  def putOrDelete[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value match {
      case Some(v) => put(store)(key, v)
      case None    => delete(store)(key)
    }

  /** aka upsert or delete */
  def putOrDelete[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value match {
      case Some(v) => put(store)(key, v)
      case None    => delete(store)(key)
    }

  /** Note: upsert */
  @inline def putOrDeleteResult[K, V](store: ObjectStoreDef.Async[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
    putOrDeleteResultBy(store)(key, ev)

  /** Note: upsert */
  @inline def putOrDeleteResult[K, V](store: ObjectStoreDef.Sync[K, V])(key: K)(implicit ev: B => Option[V]): AsyncCallback[B] =
    putOrDeleteResultBy(store)(key, ev)

  /** Note: upsert */
  def putOrDeleteResultBy[K, V](store: ObjectStoreDef.Async[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
    mapAsync { b => AsyncCallback.traverseOption(f(b))(store.encode(_)).map((b, _)) }
    .putOrDeleteResultBy(store.sync)(key, _._2)
    .map(_._1)

  /** Note: upsert */
  def putOrDeleteResultBy[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, f: B => Option[V]): AsyncCallback[B] =
    set(dsl => (_, b) =>
      for {
        s <- dsl.objectStore(store)
        _ <- s.putOrDelete(key, f(b))
      } yield b
    )

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

  /** Note: upsert */
  def putWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value.fold(AsyncCallback.unit)(put(store)(key, _))

  /** Note: upsert */
  def putWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value.fold(AsyncCallback.unit)(put(store)(key, _))

  def setConst[C](set: TxnDsl[RW] => Txn[RW, C]): AsyncCallback[C] =
    this.set(dsl => (_, _) => set(dsl))
}
