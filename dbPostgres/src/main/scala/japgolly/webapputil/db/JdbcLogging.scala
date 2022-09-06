package japgolly.webapputil.db

import com.typesafe.scalalogging.StrictLogging

object JdbcLogging extends StrictLogging with SqlTracer {

  override def logExecute(method: String, sql: String, batches: Int, err: Option[Throwable], startTimeNs: Long, endTimeNs: Long): Unit = {
    @inline def durMs = (endTimeNs - startTimeNs) / 1000000

    err match {
      case None =>
        if (batches == 1)
          logger.info(s"$method($sql) completed in $durMs ms")
        else
          logger.info(s"$method($sql) with $batches batches completed in $durMs ms")

      case Some(t) =>
        if (batches == 1)
          logger.error(s"$method($sql) failed after $durMs ms", t)
        else
          logger.error(s"$method($sql) with $batches batches failed after $durMs ms", t)
    }
  }

}
