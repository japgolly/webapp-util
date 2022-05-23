package japgolly.webapputil

package object indexeddb {

  type TxnDsl[M <: TxnMode] = japgolly.webapputil.indexeddb.dsl.TxnDsl[M]
  val TxnDsl                = japgolly.webapputil.indexeddb.dsl.TxnDsl

  type TxnDslRO = TxnDsl.RO.type
  type TxnDslRW = TxnDsl.RW.type

  @inline def TxnDslRO: TxnDslRO = TxnDsl.RO
  @inline def TxnDslRW: TxnDslRW = TxnDsl.RW

}
