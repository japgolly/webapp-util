package japgolly.webapputil.binary

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.Try

/** A means of binary compression and decompression. */
final case class Compression(compress  : BinaryData => BinaryData,
                             decompress: BinaryData => Try[BinaryData]) {

  def decompressOrThrow: BinaryData => BinaryData =
    decompress(_).get
}

object Compression {

  object ViaPako {

    /** @param level Compression level [1-9]
      * @param addHeaders Add header and adler32 crc
      */
    def apply(level: Int, addHeaders: Boolean)(implicit pako: Pako): Compression = {
      val deflateOptions = js.Dynamic.literal().asInstanceOf[Pako.DeflateOptions]
      deflateOptions.level = level
      if (addHeaders)
        Compression(
          compress   = data => pako.deflate(data, deflateOptions),
          decompress = data => Try(pako.inflate(data)),
        )
      else
        Compression(
          compress   = data => pako.deflateRaw(data, deflateOptions),
          decompress = data => Try(pako.inflateRaw(data)),
        )
    }

    def maxWithoutHeaders(implicit pako: Pako): Compression =
      apply(level = 9, addHeaders = false)

    def maxWithHeaders(implicit pako: Pako): Compression =
      apply(level = 9, addHeaders = true)

    private implicit def binaryDataToPakoData(b: BinaryData): Pako.Data =
      b.unsafeUint8Array

    private implicit def pakoDataToBinaryData(d: Pako.Data): BinaryData =
      BinaryData.unsafeFromUint8Array(d.asInstanceOf[Uint8Array])
  }
}
