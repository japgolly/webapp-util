package japgolly.webapputil.indexeddb

import org.scalajs.dom._

trait KeyRange {
  def raw: IDBKeyRange
}

// See https://developer.mozilla.org/en-US/docs/Web/API/IDBKeyRange
object KeyRange {

  final class Raw(val raw: IDBKeyRange) extends KeyRange

  final class LowerBound[K](codec: KeyCodec[K], lowerKey: IDBKey, lowerOpen: Boolean) extends KeyRange with Dsl.Upper[K, KeyRange] {

    override def raw: IDBKeyRange =
      IDBKeyRange.lowerBound(lowerKey, lowerOpen)

    override protected def upper(k: K, upperOpen: Boolean): KeyRange = {
      val upperKey = codec.encode(k).asJs
      val raw = IDBKeyRange.bound(lowerKey, upperKey, lowerOpen, upperOpen)
      new Raw(raw)
    }
  }

  // ===================================================================================================================

  final class Dsl[K](codec: KeyCodec[K]) extends Dsl.Lower[K, LowerBound[K]] with Dsl.Upper[K, KeyRange] {

    private def encode(k: K): IDBKey =
      codec.encode(k).asJs

    override protected def lower(k: K, open: Boolean): LowerBound[K] =
      new LowerBound(codec, encode(k), open)

    override protected def upper(k: K, open: Boolean): KeyRange =
      new Raw(IDBKeyRange.upperBound(encode(k), open))

    def only(k: K): KeyRange =
      new Raw(IDBKeyRange.only(encode(k)))
  }

  object Dsl {
    trait Lower[K, O] {
      protected def lower(k: K, open: Boolean): O
      /** All keys ≥ x */ def >=(x: K): O = lower(x, false)
      /** All keys > x */ def > (x: K): O = lower(x, true)
    }

    trait Upper[K, O] {
      protected def upper(k: K, open: Boolean): O
      /** All keys ≤ y */ def <=(y: K): O = upper(y, false)
      /** All keys < y */ def < (y: K): O = upper(y, true)
    }
  }
}
