package japgolly.webapputil.indexeddb

sealed trait TxnMode

object TxnMode {
  sealed trait RW extends TxnMode
  sealed trait RO extends RW
}
