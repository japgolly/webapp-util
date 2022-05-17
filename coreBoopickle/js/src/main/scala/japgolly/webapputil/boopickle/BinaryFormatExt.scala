package japgolly.webapputil.boopickle

import boopickle.{PickleImpl, Pickler, UnpickleImpl}
import japgolly.scalajs.react.callback.AsyncCallback
import japgolly.webapputil.binary._
import java.nio.ByteBuffer
import scala.scalajs.js

object BinaryFormatExt {

  trait Implicits {

    @inline final implicit def BinaryFormatBoopickleExt[A](self: BinaryFormat[A]): Implicits.BinaryFormatBoopickleExt[A] =
      new Implicits.BinaryFormatBoopickleExt[A](self)

    @inline final implicit def BinaryFormatBoopickleStaticExt(self: BinaryFormat.type): Implicits.BinaryFormatBoopickleStaticExt =
      new Implicits.BinaryFormatBoopickleStaticExt(self)
  }

  object Implicits extends Implicits {

    final class BinaryFormatBoopickleExt[A](private val self: BinaryFormat[A]) extends AnyVal {
      type ThisIsBinary = BinaryFormat[A] =:= BinaryFormat[BinaryData]

      def pickle[B](implicit pickler: SafePickler[B], ev: ThisIsBinary): BinaryFormat[B] =
        ev(self).xmap(pickler.decodeOrThrow)(pickler.encode)

      def pickleBasic[B](implicit pickler: Pickler[B], ev: ThisIsBinary): BinaryFormat[B] = {
        val unpickle = UnpickleImpl[B]
        ev(self)
          .xmap[ByteBuffer](_.unsafeByteBuffer)(BinaryData.unsafeFromByteBuffer)
          .xmap(unpickle.fromBytes(_))(PickleImpl.intoBytes(_))
      }
    }

    final class BinaryFormatBoopickleStaticExt(private val self: BinaryFormat.type) extends AnyVal {
      @inline def pickleCompressEncrypt[A](c: Compression, e: Encryption)(implicit pickler: SafePickler[A]): BinaryFormat[A] =
        BinaryFormatExt.pickleCompressEncrypt(c, e)

      @inline def versioned[A](oldest: BinaryFormat[A], toLatest: BinaryFormat[A]*): BinaryFormat[A] =
        BinaryFormatExt.versioned(oldest, toLatest: _*)
    }
  }

  // ===================================================================================================================

  def versioned[A](oldest: BinaryFormat[A], toLatest: BinaryFormat[A]*): BinaryFormat[A] = {
    val layers          = oldest +: toLatest.toArray
    val decoders        = layers
    val decoderIndices  = decoders.indices
    val latestVer       = decoders.length - 1
    val latestVerHeader = BinaryData.byte(latestVer.toByte)
    val encoder         = layers.last

    def encode(a: A): AsyncCallback[BinaryData] =
      encoder.encode(a).map(latestVerHeader ++ _)

    def decode(bin: BinaryData): AsyncCallback[A] =
      AsyncCallback.suspend {

        if (bin.isEmpty)
          throw js.JavaScriptException("No data")

        val ver = bin.unsafeArray(0).toInt

        if (decoderIndices.contains(ver)) {
          val binBody = bin.drop(1)
          decoders(ver).decode(binBody)
        } else if (ver < 0)
          throw js.JavaScriptException("Bad data")
        else
          SafePicklerUtil.unsupportedVer(ver, latestVer)
      }

    BinaryFormat.async(decode)(encode)
  }

  def pickleCompressEncrypt[A](c: Compression, e: Encryption)(implicit pickler: SafePickler[A]): BinaryFormat[A] =
    BinaryFormat.id
      .encrypt(e)  // 3. Encryption is the very last step
      .compress(c) // 2. Compress the binary *before* encrypting
      .pickle[A]   // 1. Generate binary first

}
