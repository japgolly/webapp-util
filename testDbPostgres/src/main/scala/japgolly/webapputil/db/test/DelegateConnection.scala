package japgolly.webapputil.db.test

import java.sql.{Array => _, _}
import java.util
import java.util.Properties
import java.util.concurrent.Executor

class DelegateConnection(delegate: Connection) extends Connection {
  override def abort(executor: Executor): Unit = delegate.abort(executor)
  override def clearWarnings(): Unit = delegate.clearWarnings()
  override def close(): Unit = delegate.close()
  override def commit(): Unit = delegate.commit()
  override def createArrayOf(typeName: String, elements: Array[AnyRef]): java.sql.Array = delegate.createArrayOf(typeName, elements)
  override def createBlob(): Blob = delegate.createBlob()
  override def createClob(): Clob = delegate.createClob()
  override def createNClob(): NClob = delegate.createNClob()
  override def createSQLXML(): SQLXML = delegate.createSQLXML()
  override def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): Statement = delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability)
  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): Statement = delegate.createStatement(resultSetType, resultSetConcurrency)
  override def createStatement(): Statement = delegate.createStatement()
  override def createStruct(typeName: String, attributes: Array[AnyRef]): Struct = delegate.createStruct(typeName, attributes)
  override def getAutoCommit: Boolean = delegate.getAutoCommit
  override def getCatalog: String = delegate.getCatalog
  override def getClientInfo(name: String): String = delegate.getClientInfo(name)
  override def getClientInfo: Properties = delegate.getClientInfo
  override def getHoldability: Int = delegate.getHoldability
  override def getMetaData: DatabaseMetaData = delegate.getMetaData
  override def getNetworkTimeout: Int = delegate.getNetworkTimeout
  override def getSchema: String = delegate.getSchema
  override def getTransactionIsolation: Int = delegate.getTransactionIsolation
  override def getTypeMap: util.Map[String, Class[_]] = delegate.getTypeMap
  override def getWarnings: SQLWarning = delegate.getWarnings
  override def isClosed: Boolean = delegate.isClosed
  override def isReadOnly: Boolean = delegate.isReadOnly
  override def isValid(timeout: Int): Boolean = delegate.isValid(timeout)
  override def isWrapperFor(iface: Class[_]): Boolean = delegate.isWrapperFor(iface)
  override def nativeSQL(sql: String): String = delegate.nativeSQL(sql)
  override def prepareCall(sql: String): CallableStatement = delegate.prepareCall(sql)
  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): CallableStatement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency)
  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): CallableStatement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability)
  override def prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement = delegate.prepareStatement(sql, autoGeneratedKeys)
  override def prepareStatement(sql: String, columnIndexes: Array[Int]): PreparedStatement = delegate.prepareStatement(sql, columnIndexes)
  override def prepareStatement(sql: String, columnNames: Array[String]): PreparedStatement = delegate.prepareStatement(sql, columnNames)
  override def prepareStatement(sql: String): PreparedStatement = delegate.prepareStatement(sql)
  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): PreparedStatement = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency)
  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): PreparedStatement = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability)
  override def releaseSavepoint(savepoint: Savepoint): Unit = delegate.releaseSavepoint(savepoint)
  override def rollback(savepoint: Savepoint): Unit = delegate.rollback(savepoint)
  override def rollback(): Unit = delegate.rollback()
  override def setAutoCommit(autoCommit: Boolean): Unit = delegate.setAutoCommit(autoCommit)
  override def setCatalog(catalog: String): Unit = delegate.setCatalog(catalog)
  override def setClientInfo(name: String, value: String): Unit = delegate.setClientInfo(name, value)
  override def setClientInfo(properties: Properties): Unit = delegate.setClientInfo(properties)
  override def setHoldability(holdability: Int): Unit = delegate.setHoldability(holdability)
  override def setNetworkTimeout(executor: Executor, milliseconds: Int): Unit = delegate.setNetworkTimeout(executor, milliseconds)
  override def setReadOnly(readOnly: Boolean): Unit = delegate.setReadOnly(readOnly)
  override def setSavepoint(name: String): Savepoint = delegate.setSavepoint(name)
  override def setSavepoint(): Savepoint = delegate.setSavepoint()
  override def setSchema(schema: String): Unit = delegate.setSchema(schema)
  override def setTransactionIsolation(level: Int): Unit = delegate.setTransactionIsolation(level)
  override def setTypeMap(map: util.Map[String, Class[_]]): Unit = delegate.setTypeMap(map)
  override def unwrap[T](iface: Class[T]): T = delegate.unwrap[T](iface)
}
