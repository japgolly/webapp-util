package japgolly.webapputil.cats.effect

import cats.effect.unsafe.{IORuntime, IORuntimeConfig}
import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.Logger
import japgolly.webapputil.general.ThreadUtils
import java.time.Duration
import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.ExecutionContext

object ThreadUtilsIO {
  import ThreadUtils.{ThreadGroups, ThreadPool, ThreadPool2, newThreadPool}

  object Runtimes {
    lazy val scheduledTasks = newDefaultRuntime("ScheduledTasks")
    lazy val shutdown       = newDefaultRuntime("Shutdown")
  }

  def newDefaultRuntime(threadPrefix: String): IORuntime = {
    import IORuntime._
    val (compute, _) = createDefaultComputeThreadPool(global, threadPrefix = s"$threadPrefix-compute")
    val (blocking, _) = createDefaultBlockingExecutionContext(threadPrefix = s"$threadPrefix-blocking")
    val (scheduler, _) = createDefaultScheduler(threadPrefix = s"$threadPrefix-scheduler")
    IORuntime(compute, blocking, scheduler, () => (), IORuntimeConfig())
  }

  def runOnShutdown(name: String, proc: => Unit): Unit =
    runOnShutdown(name, IO(proc))

  def runOnShutdown(name: String, task: IO[Unit]): Unit = {
    val t = new Thread(ThreadGroups.shutdown, task.toJavaRunnable(Runtimes.shutdown), "shutdown-" + name)
    java.lang.Runtime.getRuntime.addShutdownHook(t)
  }

  // ===================================================================================================================

  def resThreadPool(threadGroupName: String, logger: Logger)(f: ThreadPool => ThreadPool2): Resource[IO, ExecutionContext] =
    Resource.make[IO, ThreadPool2](
      IO(f(newThreadPool(threadGroupName, logger)))
    )(
      a => IO(a.threadPoolExecutor.shutdown())
    ).map(_.executionContext)

  // ===================================================================================================================

  def newScheduler(threadName: String, threadGroup: ThreadGroup): Scheduler =
    new Scheduler(threadName, threadGroup)

  final class Scheduler(threadName: String, threadGroup: ThreadGroup) {

    val executorService =
      Executors.newSingleThreadScheduledExecutor(new Thread(threadGroup, _, threadName))

    def scheduleAtFixedRate[A](io: IO[A], period: Duration): Scheduler =
      scheduleAtFixedRate(io, period, period)

    def scheduleAtFixedRate[A](io: IO[A], initialDelay: Duration, period: Duration): Scheduler = {
      executorService.scheduleAtFixedRate(
        io.toJavaRunnable(Runtimes.scheduledTasks),
        initialDelay.toMillis,
        period.toMillis,
        TimeUnit.MILLISECONDS)
      this
    }

    def addShutdownHook(io: IO[Unit]): Scheduler = {
      runOnShutdown(threadName, io)
      this
    }
  }
}
