package kotlinx.benchmark.jvm

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean


class NoopExecutorService(
  private val maxThreads: Int,
  private val prefix: String?,
) : ExecutorService {
  private val isShutdown = AtomicBoolean(false)
  private val isTerminated = AtomicBoolean(false)

  override fun shutdown() {
    System.err.println("$prefix NoopExecutorService.shutdown()")
    isShutdown.set(true)
    // Do nothing
  }

  override fun shutdownNow(): List<Runnable> {
    System.err.println("$prefix NoopExecutorService.shutdownNow()")
    isShutdown.set(true)
    return ArrayList()
  }

  override fun isShutdown(): Boolean {
    System.err.println("$prefix NoopExecutorService.isShutdown()")
    return isShutdown.get()
  }

  override fun isTerminated(): Boolean {
    System.err.println("$prefix NoopExecutorService.isTerminated()")
    return isTerminated.get()
  }

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
    System.err.println("$prefix NoopExecutorService.awaitTermination(timeout: $timeout, unit: $unit)")
    isTerminated.set(true)
    return isTerminated.get()
  }

  override fun <T> submit(task: Callable<T>): Future<T> {
    System.err.println("$prefix NoopExecutorService.submit(task: $task)")
    return CompletableFuture.supplyAsync { task.call() }
//    val ft = FutureTask(task)
//    Thread(ft).start()
//    return ft
  }

  override fun execute(command: Runnable) {
    System.err.println("$prefix NoopExecutorService.execute(command: $command)")

    command.run()
//    Thread(command).start()
  }

  override fun <T> submit(task: Runnable, result: T): Future<T> {
    System.err.println("$prefix NoopExecutorService.submit(task: $task, result: $result)")
    task.run()
    return CompletableFuture.completedFuture(result)
//    throw java.lang.UnsupportedOperationException()
  }

  override fun submit(task: Runnable): Future<*> {
    System.err.println("$prefix NoopExecutorService.submit(task: $task)")
    task.run()
    // return null upon *successful* completion
    return CompletableFuture.completedFuture(null)
  }

  override fun <T> invokeAll(tasks: Collection<Callable<T>?>): List<Future<T>> {
    System.err.println("$prefix NoopExecutorService.invokeAll(tasks: $tasks)")
    throw java.lang.UnsupportedOperationException()
  }

  override fun <T> invokeAll(tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): List<Future<T>> {
    System.err.println("$prefix NoopExecutorService.invokeAll(tasks: $tasks, timeout: $timeout, unit: $unit)")
    throw java.lang.UnsupportedOperationException()
  }

  override fun <T : Any> invokeAny(tasks: Collection<Callable<T>>): T {
    System.err.println("$prefix NoopExecutorService.invokeAny(tasks: $tasks)")
    throw java.lang.UnsupportedOperationException()
  }

  override fun <T> invokeAny(tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): T {
    System.err.println("$prefix NoopExecutorService.invokeAny(tasks: $tasks, timeout: $timeout, unit: $unit)")
    throw java.lang.UnsupportedOperationException()
  }
}
