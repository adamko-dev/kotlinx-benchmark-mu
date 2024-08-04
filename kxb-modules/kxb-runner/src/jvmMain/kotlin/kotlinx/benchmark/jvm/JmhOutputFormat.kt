package kotlinx.benchmark.jvm

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.benchmark.BenchmarkProgress
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import org.openjdk.jmh.infra.BenchmarkParams
import org.openjdk.jmh.infra.IterationParams
import org.openjdk.jmh.results.BenchmarkResult
import org.openjdk.jmh.results.IterationResult
import org.openjdk.jmh.results.RunResult
import org.openjdk.jmh.results.format.ResultFormatFactory
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.IterationType

@KotlinxBenchmarkRuntimeInternalApi
class JmhOutputFormat(private val reporter: BenchmarkProgress, private val suiteName: String) :
  PrintOutputFormat(System.out) {

  internal var lastBenchmarkStart = ""

  override fun startRun() {
    reporter.startSuite(suiteName)
  }

  override fun endRun(result: Collection<RunResult>) {
    val summary = ByteArrayOutputStream().apply {
      PrintStream(this, true, "UTF-8").use {
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, it).writeOut(result)
      }
    }.toString("UTF-8")
    reporter.endSuite(suiteName, summary)
  }

  override fun startBenchmark(benchParams: BenchmarkParams) {
    val benchmarkId = getBenchmarkId(benchParams)
    reporter.startBenchmark(suiteName, benchmarkId)
    lastBenchmarkStart = benchmarkId
  }

  override fun endBenchmark(result: BenchmarkResult?) {
    if (result != null) {
      val benchmarkId = getBenchmarkId(result.params)
      val value = result.primaryResult
      val message = value.extendedInfo().trim()
      reporter.endBenchmark(suiteName, benchmarkId, BenchmarkProgress.FinishStatus.Success, message)
    } else {
      reporter.endBenchmarkException(suiteName, lastBenchmarkStart, "<ERROR>", "")
    }
  }

  private fun getBenchmarkId(params: BenchmarkParams): String {
    val benchmarkName = params.benchmark
    val paramKeys = params.paramsKeys
    val benchmarkId = if (paramKeys.isEmpty())
      benchmarkName
    else
      benchmarkName + paramKeys.joinToString(prefix = " | ") { "$it=${params.getParam(it)}" }
    return benchmarkId
  }

  override fun iteration(benchParams: BenchmarkParams, params: IterationParams, iteration: Int) {}

  override fun iterationResult(
    benchParams: BenchmarkParams,
    params: IterationParams,
    iteration: Int,
    data: IterationResult
  ) {
    when (params.type) {
      IterationType.WARMUP      -> println("Warm-up $iteration: ${data.primaryResult}")
      IterationType.MEASUREMENT -> println("Iteration $iteration: ${data.primaryResult}")
      null                      -> throw UnsupportedOperationException("Iteration type not set")
    }
    flush()
  }

  override fun println(s: String) {
    if (!s.startsWith("#"))
      reporter.output(suiteName, lastBenchmarkStart, s)
  }
}
