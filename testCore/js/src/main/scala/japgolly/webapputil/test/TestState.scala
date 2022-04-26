package japgolly.webapputil.test

trait TestState extends teststate.Exports {
  type Id[A] = A
}

object TestState extends TestState
