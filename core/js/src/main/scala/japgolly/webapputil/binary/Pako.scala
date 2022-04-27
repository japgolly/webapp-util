package japgolly.webapputil.binary

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

/** Facade for the JS `pako` library with provides zlib compression & decompression. */
@js.native
@nowarn("msg=dead|never used")
sealed trait Pako extends js.Object {
  import Pako._

  def deflate(data: Data, options: DeflateOptions = js.native): Data = js.native

  /** The same as deflate, but creates raw data, without wrapper (header and adler32 crc). */
  def deflateRaw(data: Data, options: DeflateOptions = js.native): Data = js.native

  /** Throws an exception on error */
  def inflate(data: Data, options: InflateOptions = js.native): Data = js.native

  /** The same as inflate, but creates raw data, without wrapper (header and adler32 crc).
    *
    * Throws an exception on error.
    */
  def inflateRaw(data: Data, options: InflateOptions = js.native): Data = js.native
}

@nowarn("msg=dead|never used")
object Pako {

  type Data = Uint8Array | js.Array[Int] | String

  /** See http://zlib.net/manual.html#Advanced */
  @js.native
  trait DeflateOptions extends js.Object {
    /** Z_NO_COMPRESSION:         0
      * Z_BEST_SPEED:             1
      * Z_BEST_COMPRESSION:       9
      * Z_DEFAULT_COMPRESSION:   -1
      */
    var level     : js.UndefOr[Int] = js.native
    var windowBits: js.UndefOr[Int] = js.native
    var memLevel  : js.UndefOr[Int] = js.native
    var strategy  : js.UndefOr[Int] = js.native
    var dictionary: js.UndefOr[Int] = js.native
  }


  @js.native
  trait InflateOptions extends js.Object {
    var windowBits: js.UndefOr[Int] = js.native
  }

  // ===================================================================================================================

  def global: Pako =
    apply(js.Dynamic.global.pako)

  def apply(jsInstance: Any): Pako = {
    assert(js.typeOf(jsInstance) == "object", "JS object expected. Got: " + jsInstance)
    val d = jsInstance.asInstanceOf[js.Dynamic]
    assert(js.typeOf(d.deflate) == "function", ".deflate is not a function")
    force(jsInstance)
  }

  def force(jsInstance: Any): Pako =
    jsInstance.asInstanceOf[Pako]
}
