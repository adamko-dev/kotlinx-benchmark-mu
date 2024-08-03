package kotlinx.benchmark

import kotlinx.serialization.Serializable

/**
 * Benchmark mode.
 */
@Serializable
sealed class BenchmarkMode {
  internal abstract val id: String
  internal abstract val description: String
  /**
   * Throughput: operations per unit of time.
   *
   * Runs by continuously calling [Benchmark] methods,
   * counting the total throughput over all worker threads. This mode is time-based, and it will
   * run until the iteration time expires.
   */
  @Serializable
  object Throughput : BenchmarkMode() {
    override val id = "thrpt"
    override val description = "Throughput, ops/time"
    override fun toString(): String = "kotlinx.benchmark.BenchmarkMode.Throughput"
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
  @Serializable
  object AverageTime : BenchmarkMode() {
    override val id = "avgt"
    override val description = "Average time, time/op"
    override fun toString(): String = "kotlinx.benchmark.BenchmarkMode.AverageTime"
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
  @Serializable
  object SampleTime : BenchmarkMode() {
    override val id = "sample"
    override val description = "Sampling time"
    override fun toString(): String = "kotlinx.benchmark.BenchmarkMode.SampleTime"
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
  @Serializable
  object SingleShotTime : BenchmarkMode() {
    override val id = "ss"
    override val description = "Single shot invocation time"
    override fun toString(): String = "kotlinx.benchmark.BenchmarkMode.SingleShotTime"
  }

  /**
   * Meta-mode: all the benchmark modes.
   * This is mostly useful for internal JMH testing.
   */
  @Serializable
  object All : BenchmarkMode() {
    override val id = "all"
    override val description = "All benchmark modes"
    override fun toString(): String = "kotlinx.benchmark.BenchmarkMode.All"
  }

  @Serializable
  class Custom(
    override val id: String,
    override val description: String,
  ) : BenchmarkMode() {

    override fun toString(): String =
      "kotlinx.benchmark.BenchmarkMode.Custom(id=$id, description=$description)"

    override fun equals(other: Any?): Boolean =
      this === other || (other is Custom && id == other.id && description == other.description)

    override fun hashCode(): Int {
      var result = "kotlinx.benchmark.BenchmarkMode.Custom".hashCode()
      result = 31 * result + description.hashCode()
      result = 31 * result + id.hashCode()
      return result
    }
  }
}
