package japgolly.webapputil.indexeddb.dsl

import japgolly.scalajs.react._
import japgolly.webapputil.indexeddb._

final class AtomicAsyncDsl[K, V](db: Database, store: ObjectStoreDef.Async[K, V]) {

  /** Performs an async modification on a store value.
    *
    * This only modifies an existing value. Use [[modifyAsyncOption()]] to upsert and/or delete values.
    *
    * This uses [[compareAndSet()]] for atomicity and thread-safety.
    *
    * @return If the value exists, this returns the previous and updated values
    */
  def modify(key: K)(f: V => V): AsyncCallback[Option[(V, V)]] =
    modifyAsync(key)(v => AsyncCallback.pure(f(v)))

  /** Performs an async modification on a store value.
    *
    * This only modifies an existing value. Use [[modifyAsyncOption()]] to upsert and/or delete values.
    *
    * This uses [[compareAndSet()]] for atomicity and thread-safety.
    *
    * @return If the value exists, this returns the previous and updated values
    */
  def modifyAsync(key: K)(f: V => AsyncCallback[V]): AsyncCallback[Option[(V, V)]] =
    db
      .compareAndSet(store)
      .getValueAsync(store)(key)
      .mapAsync(AsyncCallback.traverseOption(_)(v1 => f(v1).map((v1, _))))
      .putResultWhenDefinedBy(store)(key, _.map(_._2))

  /** Performs an async modification on an optional store value.
    *
    * This uses [[compareAndSet()]] for atomicity and thread-safety.
    *
    * @return The previous and updated values
    */
  def modifyOption(key: K)(f: Option[V] => Option[V]): AsyncCallback[(Option[V], Option[V])] =
    modifyOptionAsync(key)(v => AsyncCallback.pure(f(v)))

  /** Performs an async modification on an optional store value.
    *
    * This uses [[compareAndSet()]] for atomicity and thread-safety.
    *
    * @return The previous and updated values
    */
  def modifyOptionAsync(key: K)(f: Option[V] => AsyncCallback[Option[V]]): AsyncCallback[(Option[V], Option[V])] =
    db
      .compareAndSet(store)
      .getValueAsync(store)(key)
      .mapAsync { o1 => f(o1).map((o1, _)) }
      .putOrDeleteResultBy(store)(key, _._2)
}

