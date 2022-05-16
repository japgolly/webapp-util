package japgolly.webapputil

package object indexeddb {

  type TxnDslRO = TxnDsl.RO.type
  type TxnDslRW = TxnDsl.RW.type

  @inline def TxnDslRO: TxnDslRO = TxnDsl.RO
  @inline def TxnDslRW: TxnDslRW = TxnDsl.RW

}
