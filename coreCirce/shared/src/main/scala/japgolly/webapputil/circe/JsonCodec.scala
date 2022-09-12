package japgolly.webapputil.circe

import cats.Traverse
import cats.instances.all._
import cats.syntax.either._
import io.circe._
import japgolly.microlibs.adt_macros.AdtMacros
import japgolly.microlibs.recursion._
import japgolly.univeq.UnivEq
import japgolly.webapputil.general.ErrorMsg
import scala.reflect.ClassTag

final case class JsonCodec[A](encoder: Encoder[A], decoder: Decoder[A]) {

  def xmap[B](f: A => B)(g: B => A): JsonCodec[B] =
    JsonCodec(encoder.contramap(g), decoder.map(f))

  @deprecated("Use .flatXmap", "2.0.0-RC3")
  def xemap[B](f: A => Decoder.Result[B])(g: B => A): JsonCodec[B] =
    flatXmap(f)(g)

  def flatXmap[B](f: A => Decoder.Result[B])(g: B => A): JsonCodec[B] =
    JsonCodec(
      encoder.contramap(g),
      Decoder.instance(decoder(_).flatMap(f)))

  def xmapOrStr[B](f: A => Either[String, B])(g: B => A): JsonCodec[B] =
    JsonCodec(
      encoder.contramap(g),
      Decoder.instance(c => decoder(c).flatMap(f(_).leftMap(DecodingFailure(_, c.history)))))

  def xmapOrErrMsg[B](f: A => Either[ErrorMsg, B])(g: B => A): JsonCodec[B] =
    xmapOrStr(f(_).leftMap(_.value))(g)

  def narrow[B <: A: ClassTag]: JsonCodec[B] =
    xmap[B]({
      case b: B => b
      case a    => throw new IllegalArgumentException("Illegal supertype: " + a)
    })(b => b)

  def inField(key: String): JsonCodec[A] = {
    val enc = encoder.mapJson(j => Json.fromJsonObject(JsonObject.singleton(key, j)))
    val dec = Decoder.instance[A](_.downField(key).as(decoder))
    JsonCodec(enc, dec)
  }

  @inline def encode(a: A): Json =
    encoder(a)

  @inline def decode(json: Json): Decoder.Result[A] =
    decoder.decodeJson(json)
}

object JsonCodec extends JsonCodecArityBoilerplate {

  @inline def apply[A](implicit j: JsonCodec[A]): JsonCodec[A] =
    j

  def summon[A](implicit encoder: Encoder[A], decoder: Decoder[A]): JsonCodec[A] =
    apply(encoder, decoder)

  lazy val boolean: JsonCodec[Boolean] = summon
  lazy val byte   : JsonCodec[Byte   ] = summon
  lazy val char   : JsonCodec[Char   ] = summon
  lazy val double : JsonCodec[Double ] = summon
  lazy val float  : JsonCodec[Float  ] = summon
  lazy val int    : JsonCodec[Int    ] = summon
  lazy val long   : JsonCodec[Long   ] = summon
  lazy val short  : JsonCodec[Short  ] = summon
  lazy val string : JsonCodec[String ] = summon
  lazy val unit   : JsonCodec[Unit   ] = summon

  @deprecated("Use JsonCodec.summon[A].xmap", "2.0.0-RC3")
  def xmap[A, B](r: A => B)(w: B => A)(implicit encoder: Encoder[A], decoder: Decoder[A]): JsonCodec[B] =
    summon[A].xmap(r)(w)

  @deprecated("Use JsonCodec.summon[A].flatXmap", "2.0.0-RC3")
  def xemap[A, B](w: B => A)(r: A => Decoder.Result[B])(implicit encoder: Encoder[A], decoder: Decoder[A]): JsonCodec[B] =
    apply(
      encoder.contramap(w),
      Decoder.instance(decoder(_).flatMap(r)))

  def lazily[A](j: => JsonCodec[A]): JsonCodec[A] = {
    lazy val l = j
    apply(
      Encoder.instance(a => l.encoder(a)),
      Decoder.instance(a => l.decoder(a)))
  }

  def const[A](a: A): JsonCodec[A] =
    apply(Encoder.encodeUnit.contramap(_ => ()), Decoder.const(a))

  def enumAdt[A, B: UnivEq](f: AdtMacros.AdtIsoSet[A, B])(implicit encoder: Encoder[B], decoder: Decoder[B]): JsonCodec[A] = {
    val mapBA = f._4.iterator.map(b => (b, Right(f._2(b)))).toMap
    JsonCodec(
      Encoder.instance(a => encoder(f._1(a))),
      Decoder.instance(c =>
        decoder(c).flatMap(b => mapBA.getOrElse(b, Left(DecodingFailure(s"Unrecognised value: $b", c.history))))))
  }

  def map[K, V](implicit
                encoderK: KeyEncoder[K],
                encoderV: Encoder[V],
                decoderK: KeyDecoder[K],
                decoderV: Decoder[V]): JsonCodec[Map[K, V]] =
    apply(Encoder.encodeMap, Decoder.decodeMap)

  def fix[F[_]: Traverse](enc: FAlgebra[F, Json],
                          dec: FCoalgebraM[Decoder.Result, F, ACursor]): JsonCodec[Fix[F]] =
    JsonCodec(
      Encoder.instance[Fix[F]](Recursion.cata(enc)(_)),
      Decoder.instance[Fix[F]](Recursion.anaM(dec)(_)))

  object FromCirceImplicitly {
    @inline implicit def implicitCirceToJsonCodecToDecoder[A: Decoder: Encoder]: JsonCodec[A] =
      JsonCodec.summon
  }

  object ToCirceImplicitly {
    @inline implicit def implicitJsonCodecToDecoder[A](implicit c: JsonCodec[A]): Decoder[A] = c.decoder
    @inline implicit def implicitJsonCodecToEncoder[A](implicit c: JsonCodec[A]): Encoder[A] = c.encoder
  }
}
