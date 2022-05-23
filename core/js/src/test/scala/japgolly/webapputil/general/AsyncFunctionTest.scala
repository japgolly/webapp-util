package japgolly.webapputil.general

import japgolly.microlibs.testutil.TypeTestingUtil._

sealed trait AsyncFunctionTest {
  type I
  type E
  type A
  def e: E

  assertType[AsyncFunction[I, E, A]]
    .map(_.attempt)
    .is[AsyncFunction[I, Either[Throwable, E], A]]

  assertType[AsyncFunction[I, Nothing, A]]
    .map(_.attempt)
    .is[AsyncFunction[I, Throwable, A]]

  assertType[AsyncFunction[I, E, A]]
    .map(_.attemptInto(_ => e))
    .is[AsyncFunction[I, E, A]]

  assertType[AsyncFunction[I, Either[E, E], A]]
    .map(_.mergeErrors)
    .is[AsyncFunction[I, E, A]]

}
