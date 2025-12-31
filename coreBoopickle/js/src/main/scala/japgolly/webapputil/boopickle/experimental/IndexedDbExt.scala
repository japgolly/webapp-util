package japgolly.webapputil.boopickle.experimental

import japgolly.webapputil.binary._
import japgolly.webapputil.boopickle.SafePickler
import japgolly.webapputil.experimental.indexeddb._

object IndexedDbExt {

  object Implicits {
    @inline implicit final class ValueCodecBoopickleExt[A](private val self: ValueCodec[A]) extends AnyVal {

      type ThisIsBinary = ValueCodec[A] =:= ValueCodec[BinaryData]

      def pickle[B](implicit pickler: SafePickler[B], ev: ThisIsBinary): ValueCodec[B] =
        ev(self).xmap(pickler.decodeOrThrow)(pickler.encode)
    }
  }

}
