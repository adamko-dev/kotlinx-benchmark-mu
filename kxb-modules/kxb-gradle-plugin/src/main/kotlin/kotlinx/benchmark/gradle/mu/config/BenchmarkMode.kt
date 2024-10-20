package kotlinx.benchmark.gradle.mu.config

/**
 * Benchmark mode.
 */
sealed class BenchmarkMode(
  internal val id: String,
  internal val description: String,
) {
  /**
   * Throughput: operations per unit of time.
   *
   * Runs by continuously calling [Benchmark] methods,
   * counting the total throughput over all worker threads. This mode is time-based, and it will
   * run until the iteration time expires.
   */
  object Throughput : BenchmarkMode("thrpt", "Throughput, ops/time") {
    override fun toString(): String = "BenchmarkMode.Throughput"
  }

  /**
   *
   * Average time: average time per operation.
   *
   *
   * Runs by continuously calling [Benchmark] methods,
   * counting the average time to call over all worker threads. This is the inverse of [Mode.Throughput],
   * but with different aggregation policy. This mode is time-based, and it will run until the iteration time
   * expires.
   */
  object AverageTime : BenchmarkMode("avgt", "Average time, time/op") {
    override fun toString(): String = "BenchmarkMode.AverageTime"
  }

  /**
   * Sample time: samples the time for each operation.
   *
   * Runs by continuously calling [Benchmark] methods, and randomly sampling the time needed for the call.
   *
   * This mode automatically adjusts the sampling frequency,
   * but may omit some pauses which missed the sampling measurement.
   *
   * This mode is time-based, and it will run until the iteration time expires.
   */
  object SampleTime : BenchmarkMode("sample", "Sampling time") {
    override fun toString(): String = "BenchmarkMode.SampleTime"
  }

  /**
   *
   * Single shot time: measures the time for a single operation.
   *
   *
   * Runs by calling [Benchmark] once and measuring its time.
   * This mode is useful to estimate the "cold" performance when you don't want to hide the warmup invocations, or
   * if you want to see the progress from call to call, or you want to record every single sample. This mode is
   * work-based, and will run only for a single invocation of [Benchmark]
   * method.
   *
   * Caveats for this mode include:
   *
   * - More warmup/measurement iterations are generally required.
   * - Timers overhead might be significant if benchmarks are small; switch to [SampleTime] mode if
   * that is a problem.
   */
  object SingleShotTime : BenchmarkMode("ss", "Single shot invocation time") {
    override fun toString(): String = "BenchmarkMode.SingleShotTime"
  }

  /**
   * Meta-mode: all the benchmark modes.
   * This is mostly useful for internal JMH testing.
   */
  object All : BenchmarkMode("all", "All benchmark modes") {
    override fun toString(): String = "BenchmarkMode.All"
  }

  class Custom(
    shortLabel: String,
    longLabel: String,
  ) : BenchmarkMode(
    id = shortLabel,
    description = longLabel,
  ) {
    override fun toString(): String = "BenchmarkMode.Custom(shortLabel='$id', longLabel='$description')"

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      return true
    }

    override fun hashCode(): Int {
      var result = "BenchmarkMode.Custom".hashCode()
      result = 31 * result + description.hashCode()
      result = 31 * result + id.hashCode()
      return result
    }
  }
}
