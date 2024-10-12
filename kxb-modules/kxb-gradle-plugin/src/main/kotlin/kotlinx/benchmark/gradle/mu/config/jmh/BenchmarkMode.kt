package kotlinx.benchmark.gradle.mu.config.jmh

/**
 * Benchmark mode.
 */
enum class BenchmarkMode(private val shortLabel: String, private val longLabel: String) {
  /**
   * Throughput: operations per unit of time.
   *
   * Runs by continuously calling [Benchmark] methods,
   * counting the total throughput over all worker threads. This mode is time-based, and it will
   * run until the iteration time expires.
   */
  Throughput("thrpt", "Throughput, ops/time"),

  /**
   * Average time: average time per operation.
   *
   * Runs by continuously calling [Benchmark] methods,
   * counting the average time to call over all worker threads. This is the inverse of [BenchmarkMode.Throughput],
   * but with different aggregation policy. This mode is time-based, and it will run until the iteration time
   * expires.
   */
  AverageTime("avgt", "Average time, time/op"),

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
  SampleTime("sample", "Sampling time"),

  /**
   *
   * Single shot time: measures the time for a single operation.
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
  SingleShotTime("ss", "Single shot invocation time"),

  /**
   * Meta-mode: all the benchmark modes.
   * This is mostly useful for internal JMH testing.
   */
  All("all", "All benchmark modes"),
  ;
}
