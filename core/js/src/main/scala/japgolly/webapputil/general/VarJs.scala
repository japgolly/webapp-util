package japgolly.webapputil.general

import japgolly.scalajs.react.callback._
import japgolly.univeq._
import japgolly.webapputil.webstorage.AbstractWebStorage
import scala.scalajs.js

/** Immutable reference to a potentially abstract, potentially mutable variable. */
final class VarJs[A](val unsafeGet: () => A, val unsafeSet: A => Unit) { self =>

  def xmap[B](f: A => B)(g: B => A): VarJs[B] =
    VarJs.viaFns(f(self.unsafeGet()))(b => self.unsafeSet(g(b)))

  def get: CallbackTo[A] =
    CallbackTo(unsafeGet())

  def set(a: A): Callback =
    Callback(unsafeSet(a))
}

object VarJs {

  def apply[A](initialState: A): VarJs[A] = {
    var a = initialState
    viaFns(a)(a2 => a = a2)
  }

  def const[A](a: A): VarJs[A] =
    new VarJs(() => a, _ => ())

  def unsafeField[A](j: Any, field: String): VarJs[A] = {
    val d = j.asInstanceOf[js.Dynamic]
    new VarJs(
      () => d.selectDynamic(field).asInstanceOf[A],
      a => d.updateDynamic(field)(a.asInstanceOf[js.Any]))
  }

  def viaFns[A](get: => A)(set: A => Unit): VarJs[A] =
    new VarJs(() => get, set)

  def viaCallbacks[A](get: CallbackTo[A])(set: A => Callback): VarJs[A] =
    viaFns(get.runNow())(set(_).runNow())

  object webStorage {
    import AbstractWebStorage.{Key, Value}

    def apply(ws: AbstractWebStorage, key: Key): VarJs[Option[Value]] =
      viaFns(ws.getItem(key).runNow())(ws.setOrRemoveItem(key, _).runNow())

    def withDefault(ws: AbstractWebStorage, key: Key, default: Value): VarJs[Value] =
      // apply(ws, key).xmap(_.getOrElse(default))(v => Option.unless(v ==* default)(v))
      apply(ws, key).xmap(_.getOrElse(default))(Some(_))

    def boolean(ws: AbstractWebStorage, key: Key, valueWhenEmpty: Boolean = false): VarJs[Boolean] = {
      val asValue = (b: Boolean) => Value(if (b) "1" else "0")
      withDefault(ws, key, asValue(valueWhenEmpty)).xmap(_.value ==* "1")(asValue)
    }
  }
}
