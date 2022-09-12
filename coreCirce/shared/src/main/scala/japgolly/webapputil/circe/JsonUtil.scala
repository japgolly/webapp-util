package japgolly.webapputil.circe

import io.circe._
import io.circe.syntax._
import japgolly.microlibs.utils.StaticLookupFn
import japgolly.webapputil.general.ErrorMsg
import japgolly.univeq._

object JsonUtil {

  object Implicits extends Implicits

  trait Implicits
    extends CirceExtensions
       with UnivEqInstances

  // ===================================================================================================================

  object UnivEqInstances extends UnivEqInstances

  trait UnivEqInstances {
    implicit def univEqCirceDecodingFailure: UnivEq[DecodingFailure] = UnivEq.force
    implicit def univEqCirceError          : UnivEq[io.circe.Error]  = UnivEq.force
    implicit def univEqCirceJson           : UnivEq[Json]            = UnivEq.force
    implicit def univEqCirceJsonObject     : UnivEq[JsonObject]      = UnivEq.force
  }

  // ===================================================================================================================

  trait CirceExtensions {
    import CirceExtensions._

    implicit def webappUtilCirceExtEncoderObject(a: Encoder.type): WebappUtilCirceExtEncoderObject =
      new WebappUtilCirceExtEncoderObject(a)

    implicit def WebappUtilCirceExtDecoderF[F[_], A](a: Decoder[F[A]]): WebappUtilCirceExtDecoderF[F, A] =
      new WebappUtilCirceExtDecoderF(a)

    implicit def webappUtilCirceExtHCursor(a: HCursor): WebappUtilCirceExtHCursor =
      new WebappUtilCirceExtHCursor(a)

    implicit def webappUtilCirceExtJsonObject(a: JsonObject): WebappUtilCirceExtJsonObject =
      new WebappUtilCirceExtJsonObject(a)
  }

  object CirceExtensions extends CirceExtensions {

    class WebappUtilCirceExtEncoderObject(private val self: Encoder.type) extends AnyVal {

      def instanceObject[A](f: A => JsonObject): Encoder[A] =
        Encoder.instance[A](a => Json.fromJsonObject(f(a)))
    }

    class WebappUtilCirceExtDecoderF[F[_], A](private val self: Decoder[F[A]]) extends AnyVal {

      def orLiftOne(f: A => F[A])(implicit A: Decoder[A]): Decoder[F[A]] =
        Decoder.instance[F[A]] { c =>
          val rf = self(c)
          if (rf.isRight)
            rf
          else {
            val ra = A(c)
            if (ra.isRight) ra.map(f) else rf
          }
        }
    }

    class WebappUtilCirceExtHCursor(private val self: HCursor) extends AnyVal {

      def decodeSomeOrOne[F[_], A](one: A => F[A])
                                  (implicit F: Decoder[F[A]], A: Decoder[A]): Decoder.Result[F[A]] =
        F.orLiftOne(one)(A)(self)

      def getOption[A: Decoder](key: String, nullIsNone: Boolean = true): Decoder.Result[Option[A]] = {
        val f = self.downField(key)
        f.focus match {
          case None                              => Right(None)
          case Some(j) if nullIsNone && j.isNull => Right(None)
          case Some(_)                           => f.as[A].map(Some.apply)
        }
      }

      def getOrDefault[A: Decoder](key: String, default: A, nullIsNone: Boolean = true): Decoder.Result[A] =
        getOption(key, nullIsNone = nullIsNone).map(_.getOrElse(default))

      def getSoleField[A: Decoder](key: String): Decoder.Result[A] =
        self.keys match {
          case Some(keys) =>
            keys.iterator.take(2).toList match {
              case k :: Nil if k ==* key => self.downField(key).as[A]
              case _                     => Left(DecodingFailure(s"Exactly one key ($key) required in ${self.focus}", self.history))
            }
          case None => Left(DecodingFailure("Object expected.", self.history))
        }

      def getSomeOrOne[F[_], A](key: String, one: A => F[A])
                               (implicit F: Decoder[F[A]], A: Decoder[A]): Decoder.Result[F[A]] =
        self.get(key)(F.orLiftOne(one))
    }

    class WebappUtilCirceExtJsonObject(private val self: JsonObject) extends AnyVal {

      def addOption[A: Encoder](key: String, value: Option[A]): JsonObject =
        value.fold(self)(a => self.add(key, a.asJson))

      def addWhen[A: Encoder](key: String, value: A)(test: A => Boolean): JsonObject =
        if (test(value)) self.add(key, value.asJson) else self
    }
  }

  // ===================================================================================================================

  def decoderFnSumBySoleKey[A](f: PartialFunction[(String, ACursor), Decoder.Result[A]]): ACursor => Decoder.Result[A] = {
    def keyErr = "Expected a single key indicating the subtype"
    c =>
      c.keys match {
        case Some(it) =>
          it.toList match {
            case singleKey :: Nil =>
              val arg  = (singleKey, c.downField(singleKey))
              def fail = Left(DecodingFailure("Unknown subtype: " + singleKey, c.history))
              f.applyOrElse(arg, (_: (String, ACursor)) => fail)
            case Nil  => Left(DecodingFailure(keyErr, c.history))
            case keys => Left(DecodingFailure(s"$keyErr, found multiple: $keys", c.history))
          }
        case None => Left(DecodingFailure(keyErr, c.history))
      }
  }

  def decodeSumBySoleKey[A](f: PartialFunction[(String, ACursor), Decoder.Result[A]]): Decoder[A] =
    Decoder.instance(decoderFnSumBySoleKey(f))

  def decodeSumBySoleKeyOrConst[A](consts: (String, A)*)(f: PartialFunction[(String, ACursor), Decoder.Result[A]]): Decoder[A] =
    decodeSumBySoleKeyOr(consts: _*)(decoderFnSumBySoleKey(f))

  def decodeSumBySoleKeyOr[A](consts: (String, A)*)(orElse: ACursor => Decoder.Result[A]): Decoder[A] = {
    val lookup = StaticLookupFn.useMap(consts).toOption
    Decoder.instance(c =>
      c.as[String].toOption.flatMap(lookup) match {
        case Some(r) => Right(r)
        case None    => orElse(c)
      }
    )
  }

  def decodingFailureMsg(f: DecodingFailure): ErrorMsg =
    ErrorMsg(s"Failed to decode JSON at ${CursorOp.opsToPath(f.history)}: ${f.message}")

  def errorMsg(e: io.circe.Error): ErrorMsg =
    e match {
      case f: ParsingFailure  => ErrorMsg(s"Failed to parse JSON: ${f.message}")
      case f: DecodingFailure => decodingFailureMsg(f)
    }

}
