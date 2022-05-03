package japgolly.webapputil.general

import japgolly.microlibs.utils.SafeBool

/** Type-safe union of `Allow | Deny` */
sealed abstract class Permission extends SafeBool.WithBoolOps[Permission] {
  override final def companion = Permission

  def apply[A](a: => A): Permission.DeniedOr[A]
  def option[A](a: => A): Option[A]
}

case object Allow extends Permission {
  override def apply[A](a: => A) = Right(a)
  override def option[A](a: => A) = Some(a)
}

case object Deny extends Permission {
  override def apply[A](a: => A) = Permission.denied
  override def option[A](a: => A) = None
}

object Permission extends SafeBool.Object[Permission] {
  override def positive = Allow
  override def negative = Deny

  type DeniedOr[+A] = Either[Deny.type, A]
  val denied: Permission.DeniedOr[Nothing] = Left(Deny)
}
