package japgolly.webapputil.boopickle

import boopickle.{PickleImpl, PickleState, Pickler, UnpickleImpl, UnpickleState}
import japgolly.scalajs.react.AsyncCallback
import japgolly.webapputil.binary.{BinaryData, Encryption}
import japgolly.webapputil.general.JsExt._
import org.scalajs.dom.{crypto => _, _}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.util.Try

/** Implementation of [[Encryption.Engine]] */
object EncryptionEngine {

  lazy val global: Option[Encryption.Engine] =
    from(js.Dynamic.global.crypto)

  def from(a: => Any): Option[Encryption.Engine] =
    try {
      val untyped = a
      Option.when(isAvailable(untyped)) {
        val crypto = untyped.asInstanceOf[Crypto]
        apply(crypto)
      }
    } catch {
      case _: Throwable => None
    }

  private def isAvailable(crypto: Any): Boolean =
    Try[Boolean] {
      val c = crypto.asInstanceOf[js.Dynamic]
      val subtle = c.subtle
      (
        (c.getRandomValues: Any).truthy
        && (subtle.importKey: Any).truthy
        && (subtle.encrypt: Any).truthy
        && (subtle.decrypt: Any).truthy
      )
    }.fold(_ => false, identity)

  // ===================================================================================================================
  import PicklerUtil.picklerBinaryDataFixedLength

  def apply(crypto: Crypto): Encryption.Engine =
    new Encryption.Engine {
      override def apply(symmetricKey: BinaryData): AsyncCallback[Encryption] = {

        val newIV: AsyncCallback[Uint8Array] =
          AsyncCallback.delay {
            val iv = new Uint8Array(16)
            crypto.getRandomValues(iv)
            iv
          }

        def algorithm(iv: Uint8Array): AlgorithmIdentifier =
          js.Dynamic.literal(
            name = "AES-GCM",
            iv   = iv,
          ).asInstanceOf[AlgorithmIdentifier]

        def encrypt(key: CryptoKey): BinaryData => AsyncCallback[BinaryData] = {

          def encryptData(iv: Uint8Array, input: BinaryData): AsyncCallback[BinaryData] =
            AsyncCallback.fromJsPromise(
              crypto.subtle.encrypt(algorithm(iv), key, input.unsafeArrayBuffer)
            ).map(a => BinaryData.unsafeFromArrayBuffer(a.asInstanceOf[ArrayBuffer]))

          input =>
            for {
              iv      <- newIV
              encData <- encryptData(iv, input)
            } yield {
              val eb = EncryptedBinary(BinaryData.unsafeFromUint8Array(iv), encData)
              val bb = PickleImpl.intoBytes(eb)
              BinaryData.unsafeFromByteBuffer(bb)
            }
        }

        def decrypt(key: CryptoKey): BinaryData => AsyncCallback[BinaryData] = {

          val unpickler = UnpickleImpl[EncryptedBinary]

          def decryptData(eb: EncryptedBinary): AsyncCallback[BinaryData] = {
            AsyncCallback.fromJsPromise(
              crypto.subtle.decrypt(algorithm(eb.iv.unsafeUint8Array), key, eb.data.unsafeArrayBuffer)
            ).map(a => BinaryData.unsafeFromArrayBuffer(a.asInstanceOf[ArrayBuffer]))
          }

          pickled =>
            for {
              eb        <- AsyncCallback.delay(unpickler.fromBytes(pickled.unsafeByteBuffer))
              decrypted <- decryptData(eb)
            } yield decrypted
        }

        val importKey: AsyncCallback[CryptoKey] =
          AsyncCallback.fromJsPromise(
            crypto.subtle.importKey(
              format      = KeyFormat.raw,
              keyData     = symmetricKey.unsafeArrayBuffer,
              algorithm   = "AES-GCM",
              extractable = false,
              keyUsages   = js.Array(KeyUsage.encrypt, KeyUsage.decrypt),
            )
          )

        val main: AsyncCallback[Encryption] =
          for {
            key <- importKey
          } yield
            Encryption(
              encrypt = encrypt(key),
              decrypt = decrypt(key),
            )

        main.memo()
      }
    }

  private case class EncryptedBinary(iv: BinaryData, data: BinaryData) {
    def length = iv.length + data.length
  }

  private object Version1 {

    // Yes, 16 bytes and not 12. See https://shipreq.com/project/d6My#/reqs/IV-42
    final val IvLength = 16

    object pickler extends Pickler[EncryptedBinary] {

      private val ivEncoder = picklerBinaryDataFixedLength(IvLength)

      override def pickle(a: EncryptedBinary)(implicit state: PickleState): Unit = {
        state.enc.writeInt(a.length)
        state.pickle(a.iv)(ivEncoder)
        state.pickle(a.data)(picklerBinaryDataFixedLength(a.data.length))
      }

      override def unpickle(implicit state: UnpickleState): EncryptedBinary = {
        val len  = state.dec.readInt
        val iv   = state.unpickle(ivEncoder)
        val data = state.unpickle(picklerBinaryDataFixedLength(len - IvLength))
        EncryptedBinary(iv, data)
      }
    }
  }

  private implicit val picklerEncryptedBinary: Pickler[EncryptedBinary] =
    new Pickler[EncryptedBinary] {

      @inline private def LatestVer: Byte = 0 // ver 1.0

      override def pickle(a: EncryptedBinary)(implicit state: PickleState): Unit = {
        state.enc.writeByte(LatestVer)
        state.pickle(a)(Version1.pickler)
      }

      override def unpickle(implicit state: UnpickleState): EncryptedBinary =
        state.dec.readByte match {
          case 0 => state.unpickle(Version1.pickler)
          case v => SafePicklerUtil.unsupportedVer(v.toInt, LatestVer)
        }
    }
}
