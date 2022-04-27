package japgolly.webapputil.webstorage

import japgolly.scalajs.react.{AsyncCallback, Callback, CallbackTo}
import japgolly.webapputil.binary._
import japgolly.webapputil.webstorage.AbstractWebStorage.Key

final case class WebStorageKey[V](key: Key, valueCodec: ValueCodec[V]) {

  def get(implicit s: AbstractWebStorage): CallbackTo[Option[V]] =
    s.getItem(key).flatMap(CallbackTo.traverseOption(_)(valueCodec.decode))

  def set(value: V)(implicit s: AbstractWebStorage): Callback =
    valueCodec.encode(value).flatMap(s.setItem(key, _))

  def remove(implicit s: AbstractWebStorage): Callback =
    s.removeItem(key)

  def setOrRemove(value: Option[V])(implicit s: AbstractWebStorage): Callback =
    value.fold(remove)(set(_))
}

object WebStorageKey {

  final case class Async[V](key: Key, valueCodec: ValueCodec.Async[V]) {

    def get(implicit s: AbstractWebStorage): AsyncCallback[Option[V]] =
      s.getItem(key).asAsyncCallback.flatMap(AsyncCallback.traverseOption(_)(valueCodec.decode))

    def set(value: V)(implicit s: AbstractWebStorage): AsyncCallback[Unit] =
      valueCodec.encode(value).flatMap(s.setItem(key, _).asAsyncCallback)

    def remove(implicit s: AbstractWebStorage): Callback =
      s.removeItem(key)

    def setOrRemove(value: Option[V])(implicit s: AbstractWebStorage): AsyncCallback[Unit] =
      value.fold(remove.asAsyncCallback)(set(_))
  }

  // ===================================================================================================================
  // Convenience methods

  def string(key: String): WebStorageKey[String] =
    new WebStorageKey(Key(key), ValueCodec.string)

  def boolean(key: String): WebStorageKey[Boolean] =
    new WebStorageKey(Key(key), ValueCodec.boolean)

  def binaryString(key: String)(implicit enc: BinaryString.Encoder): WebStorageKey[BinaryString] =
    new WebStorageKey(Key(key), ValueCodec.binaryString)

  def binary(key: String)(implicit enc: BinaryString.Encoder): WebStorageKey[BinaryData] =
    new WebStorageKey(Key(key), ValueCodec.binary)

  object Async {

    def binary(key: String)(implicit enc: BinaryString.Encoder): Async[BinaryData] =
      Async(Key(key), ValueCodec.Async.binary)

    def binaryFormat[A](key: String, fmt: BinaryFormat[A])(implicit enc: BinaryString.Encoder): Async[A] =
      Async(Key(key), ValueCodec.Async.binaryFormat(fmt))
  }
}
