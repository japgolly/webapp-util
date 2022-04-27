package japgolly.webapputil.binary

import japgolly.scalajs.react.AsyncCallback

/** A means of converting instances of type `A` to a binary format and back. */
final class BinaryFormat[A](val encode: A => AsyncCallback[BinaryData],
                            val decode: BinaryData => AsyncCallback[A]) {

  def xmap[B](onDecode: A => B)(onEncode: B => A): BinaryFormat[B] =
  // Delegating because decoding can fail and must be wrapped to be pure
    xmapAsync(
      a => AsyncCallback.delay(onDecode(a)))(
      b => AsyncCallback.delay(onEncode(b)))

  def xmapAsync[B](onDecode: A => AsyncCallback[B])(onEncode: B => AsyncCallback[A]): BinaryFormat[B] =
    BinaryFormat.async(
      decode(_).flatMap(onDecode))(
      onEncode(_).flatMap(encode))

  type ThisIsBinary = BinaryFormat[A] =:= BinaryFormat[BinaryData]

  def encrypt(e: Encryption)(implicit ev: ThisIsBinary): BinaryFormat[BinaryData] =
    ev(this).xmapAsync(e.decrypt)(e.encrypt)

  def compress(c: Compression)(implicit ev: ThisIsBinary): BinaryFormat[BinaryData] =
    ev(this).xmap(c.decompressOrThrow)(c.compress)
}

object BinaryFormat {

  val id: BinaryFormat[BinaryData] = {
    val f: BinaryData => AsyncCallback[BinaryData] = AsyncCallback.pure
    async(f)(f)
  }

  def apply[A](decode: BinaryData => A)
              (encode: A => BinaryData): BinaryFormat[A] =
    async(
      b => AsyncCallback.delay(decode(b)))(
      a => AsyncCallback.delay(encode(a)))

  def async[A](decode: BinaryData => AsyncCallback[A])
              (encode: A => AsyncCallback[BinaryData]): BinaryFormat[A] =
    new BinaryFormat(encode, decode)
}
