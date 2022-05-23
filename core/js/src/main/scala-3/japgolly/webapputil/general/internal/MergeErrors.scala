package japgolly.webapputil.general.internal

import scala.annotation._
import scala.compiletime._

@implicitNotFound("Don't know how to merge Either[${E1}, ${E2}]")
trait MergeErrors[-E1, -E2] {
  type E
  val merge: Either[E1, E2] => E
}

object MergeErrors extends MergeErrors2 {
  type To[-E1, -E2, EE] = MergeErrors[E1, E2] { type E = EE }

  def apply[A, B, C](f: Either[A, B] => C): To[A, B, C] =
    new MergeErrors[A, B] {
      override type E = C
      override val merge = f
    }

  @nowarn("msg=match may not be exhaustive")
  implicit def nothingRight[A]: MergeErrors.To[A, Nothing, A] =
    MergeErrors[A, Nothing, A] { case Left(a) => a }
}

trait MergeErrors2 extends MergeErrors1 {
  @nowarn("msg=match may not be exhaustive")
  implicit def nothingLeft[A]: MergeErrors.To[Nothing, A, A] =
    MergeErrors[Nothing, A, A] { case Right(a) => a }
}

trait MergeErrors1 {
  @nowarn("msg=match may not be exhaustive")
  transparent inline implicit def derive[A, B]: MergeErrors[A, B] =
    summonFrom {
      case ev: (A =:= B) => MergeErrors[A, B, B](_.fold(ev, identity))
      case _             => MergeErrors[A, B, Either[A, B]](identity)
    }
}
