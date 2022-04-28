package japgolly.webapputil.boopickle

import boopickle.Decoder
import boopickle.DefaultBasic._
import cats.Functor
import cats.data.Ior
import japgolly.microlibs.nonempty._
import japgolly.microlibs.recursion._
import japgolly.microlibs.stdlib_ext.StdlibExt._
import japgolly.microlibs.utils.SafeBool
import japgolly.univeq.UnivEq
import japgolly.webapputil.binary.BinaryData
import japgolly.webapputil.general.ErrorMsg
import java.nio.ByteBuffer
import java.time.Instant
import scala.annotation.nowarn
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

object PicklerUtil {

  // ===================================================================================================================
  // Extension classes

  object Implicits {

    @inline implicit def boopickleUtilAnyRefPicklerExt[A <: AnyRef](a: Pickler[A]): AnyRefPicklerExt[A] =
      new AnyRefPicklerExt[A](a)

    @inline implicit def boopickleUtilDecoderExt(a: Decoder): DecoderExt =
      new DecoderExt(a)
  }

  implicit final class AnyRefPicklerExt[A <: AnyRef](private val p: Pickler[A]) extends AnyVal {

    @nowarn("cat=unused")
    def reuseByUnivEq(implicit ev: UnivEq[A]) =
      new PickleWithReuse[A](p, true)

    def reuseByRef =
      new PickleWithReuse[A](p, false)

    def narrow[B <: A: ClassTag]: Pickler[B] =
      p.xmap[B]({
        case b: B => b
        case a    => throw new IllegalArgumentException("Illegal supertype: " + a)
      })(b => b)
  }

  final class PickleWithReuse[A <: AnyRef](p: Pickler[A], byUnivEq: Boolean) extends Pickler[A] {
    private[this] val getP: (PickleState, A) => Option[Int] = if (byUnivEq) _ immutableRefFor _  else _ identityRefFor _
    private[this] val getU: (UnpickleState, Int) => A       = if (byUnivEq) _.immutableFor[A](_) else _.identityFor[A](_)
    private[this] val setP: (PickleState  , A) => Unit      = if (byUnivEq) _ addImmutableRef _  else _ addIdentityRef _
    private[this] val setU: (UnpickleState, A) => Unit      = if (byUnivEq) _ addImmutableRef _  else _ addIdentityRef _

    override def pickle(value: A)(implicit state: PickleState): Unit = {
      val ref = getP(state, value)
      if (ref.isDefined)
        state.enc.writeInt(-ref.get)
      else {
        state.enc.writeInt(0)
        p.pickle(value)
        setP(state, value)
      }
    }
    override def unpickle(implicit state: UnpickleState): A =
      state.dec.readIntCode match {
        case Right(i) =>
          if (i == 0) {
            val value = p.unpickle
            setU(state, value)
            value
          } else
            getU(state, -i)
        case Left(_) =>
          throw new IllegalArgumentException("Unknown coding")
      }
  }

  implicit final class DecoderExt(private val self: Decoder) extends AnyVal {
    def buf: ByteBuffer =
      self match {
        case a: boopickle.DecoderSpeed => a.buf
        case a: boopickle.DecoderSize  => a.buf
      }

    def peek[A](f: Decoder => A): A = {
      val b = buf
      val p = b.position()
      try f(self) finally b.position(p)
    }
  }

  // ===================================================================================================================
  // Polymorphic definitions
  // (non-implicit, "pickle" prefix)

  def pickleArraySeq[A](implicit pa: Pickler[A], ct: ClassTag[A]): Pickler[ArraySeq[A]] =
    // Can't use boopickle.BasicPicklers.ArrayPickler here because internally, it uses writeRawInt to write length,
    // where as IterablePickler uses writeInt. We need to be compatible because we're switching out a Vector for an
    // ArraySeq in some impls without affecting the codec.
    boopickle.BasicPicklers.IterablePickler[A, ArraySeq]

  def pickleEither[L: Pickler, R: Pickler]: Pickler[Either[L, R]] =
    new Pickler[Either[L, R]] {
      private[this] final val KeyR = 0
      private[this] final val KeyL = 1
      override def pickle(a: Either[L, R])(implicit state: PickleState): Unit =
        a match {
          case Right(r) => state.enc.writeByte(KeyR); state.pickle(r)
          case Left(l) => state.enc.writeByte(KeyL); state.pickle(l)
        }
      override def unpickle(implicit state: UnpickleState): Either[L, R] =
        state.dec.readByte match {
          case KeyR => Right(state.unpickle[R])
          case KeyL => Left(state.unpickle[L])
        }
    }

  def pickleEnum[V: UnivEq](nev: NonEmptyVector[V], firstValue: Int = 0): Pickler[V] =
    new Pickler[V] {
      private[this] val fromInt = nev.whole
      private[this] val toInt   = nev.whole.mapToOrder
      assert(toInt.size == nev.length, s"Duplicates found in $nev")
      override def pickle(v: V)(implicit state: PickleState): Unit = {
        val i = toInt(v) + firstValue
        state.enc.writeInt(i)
      }
      override def unpickle(implicit state: UnpickleState): V =
        state.dec.readIntCode match {
          case Right(i) => fromInt(i - firstValue)
          case Left(_)  => throw new IllegalArgumentException("Invalid coding")
        }
    }

  def pickleFix[F[_] : Functor](implicit p: Pickler[F[Unit]]): Pickler[Fix[F]] =
    new Pickler[Fix[F]] {
      override def pickle(f: Fix[F])(implicit state: PickleState): Unit = {

        // val fUnit = Functor[F].void(f.unfix)
        // p.pickle(fUnit)
        // Functor[F].map(f.unfix)(pickle)

        // Compared to ↑, this ↓ is generally on-par for small trees, and around 30% faster for larger, deeper trees

        val fields = new collection.mutable.ArrayBuffer[Fix[F]](32)
        val fUnit = Functor[F].map(f.unfix) { a =>
          fields += a
          ()
        }
        p.pickle(fUnit)
        fields.foreach(pickle)

        ()
      }

      override def unpickle(implicit state: UnpickleState) = {
        val fUnit = p.unpickle
        Fix(Functor[F].map(fUnit)(_ => unpickle))
      }
    }

  def pickleIor[A: Pickler, B: Pickler]: Pickler[A Ior B] =
    new Pickler[A Ior B] {
      import Ior._
      private[this] final val KeyLeft = 0
      private[this] final val KeyRight = 1
      private[this] final val KeyBoth = 2
      override def pickle(i: A Ior B)(implicit state: PickleState): Unit =
        i match {
          case Left(a)    => state.enc.writeByte(KeyLeft); state.pickle(a)
          case Right(b)   => state.enc.writeByte(KeyRight); state.pickle(b)
          case Both(a, b) => state.enc.writeByte(KeyBoth); state.pickle(a); state.pickle(b)
        }
      override def unpickle(implicit state: UnpickleState): A Ior B =
        state.dec.readByte match {
          case KeyLeft => Left(state.unpickle[A])
          case KeyRight => Right(state.unpickle[B])
          case KeyBoth =>
            val a = state.unpickle[A]
            val b = state.unpickle[B]
            Both(a, b)
        }
    }

  def pickleLazily[A](f: => Pickler[A]): Pickler[A] = {
    lazy val p = f
    new Pickler[A] {
      override def pickle(a: A)(implicit state: PickleState): Unit = p.pickle(a)
      override def unpickle(implicit state: UnpickleState)  : A    = p.unpickle
    }
  }

  def pickleMap[K: Pickler, V: Pickler]: Pickler[Map[K, V]] =
    mapPickler[K, V, Map]

  // def pickleNEA[A](implicit p: Pickler[ArraySeq[A]]): Pickler[NonEmptyArraySeq[A]] =
  //   pickleNonEmpty(_.whole)

  def pickleNES[A: UnivEq](implicit p: Pickler[Set[A]]): Pickler[NonEmptySet[A]] =
    pickleNonEmpty(_.whole)

  def pickleNEV[A](implicit p: Pickler[Vector[A]]): Pickler[NonEmptyVector[A]] =
    pickleNonEmpty(_.whole)

  def pickleNonEmpty[N, E](f: N => E)(implicit p: Pickler[E], proof: NonEmpty.Proof[E, N]): Pickler[N] =
    p.xmap(NonEmpty require_! _)(f)

  def pickleNonEmptyMono[A](implicit p: Pickler[A], proof: NonEmpty.ProofMono[A]): Pickler[NonEmpty[A]] =
    pickleNonEmpty(_.value)

  private object _pickleNothing extends Pickler[AnyRef] {
    override def pickle(obj: AnyRef)(implicit state: PickleState): Unit = ()
    override def unpickle(implicit state: UnpickleState): Nothing = throw new RuntimeException("This case is illegal.")
  }

  def pickleNothing[A <: AnyRef]: Pickler[A] =
    _pickleNothing.asInstanceOf[Pickler[A]]

  def pickleSafeBoolValues[B <: SafeBool[B], A: Pickler]: Pickler[SafeBool.Values[B, A]] =
    transformPickler[SafeBool.Values[B, A], (A, A)](x => SafeBool.Values(pos = x._1, neg = x._2))(x => (x.pos, x.neg))

  // ===================================================================================================================
  // Concrete picklers for base data type
  // (implicit lazy vals, "pickler" prefix)

  implicit lazy val picklerBinaryData: Pickler[BinaryData] =
    transformPickler(BinaryData.unsafeFromArray)(_.unsafeArray)

  def picklerBinaryDataFixedLength(len: Int): Pickler[BinaryData] =
    new Pickler[BinaryData] {

      override def pickle(bin: BinaryData)(implicit state: PickleState): Unit = {
        assert(bin.length == len)
        val enc = state.enc
        val bytes = bin.unsafeArray
        var i = 0
        while (i < len) {
          enc.writeByte(bytes(i))
          i += 1
        }
      }

      override def unpickle(implicit state: UnpickleState): BinaryData = {
        val dec = state.dec
        val bytes = new Array[Byte](len)
        var i = 0
        while (i < len) {
          bytes(i) = dec.readByte
          i += 1
        }
        BinaryData.unsafeFromArray(bytes)
      }
    }

  implicit lazy val picklerErrorMsg: Pickler[ErrorMsg] =
    transformPickler(ErrorMsg.apply)(_.value)

  implicit lazy val picklerErrorMsgOrUnit: Pickler[Either[ErrorMsg, Unit]] =
    pickleEither

  implicit lazy val picklerInstant: Pickler[Instant] =
    new Pickler[Instant] {
      // EpochSecond is stored as a packed long (typically 5 bytes instead of 8 raw)
      // Nano is stored as a raw int (4 bytes instead of typically 5 packed, P(27%) 4 packed)
      override def pickle(i: Instant)(implicit state: PickleState): Unit = {
        state.enc.writeLong(i.getEpochSecond)
        state.enc.writeRawInt(i.getNano)
      }
      override def unpickle(implicit state: UnpickleState): Instant = {
        val epochSecond = state.dec.readLong
        val nano        = state.dec.readRawInt
        Instant.ofEpochSecond(epochSecond, nano)
      }
    }

  implicit lazy val picklerNonEmptyVectorInt: Pickler[NonEmptyVector[Int]] =
    pickleNEV

}
