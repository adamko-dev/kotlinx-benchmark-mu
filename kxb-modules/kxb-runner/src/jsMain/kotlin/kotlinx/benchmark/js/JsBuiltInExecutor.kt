package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class JsBuiltInExecutor(
  name: String,
  @Suppress("UNUSED_PARAMETER")
  dummyArgs: Array<out String>,
) : CommonSuiteExecutor(
  executionName = name,
  encodedBenchmarkParameters = jsEngineSupport.arguments().first(),
) {

//  private val BenchmarkConfiguration.notUseJsBridge: Boolean
//    get() = "false".equals(advanced["jsUseBridge"], ignoreCase = true)

  override fun run(
    runnerConfiguration: RunnerConfiguration,
    benchmarks: List<BenchmarkDescriptor<Any?>>,
    start: () -> Unit,
    complete: () -> Unit
  ) {
    if (benchmarks.any { it.isAsync }) {
      error("${JsBuiltInExecutor::class.simpleName} does not support async functions")
    }
    super.run(runnerConfiguration, benchmarks, start, complete)
  }

  private fun createJsMeasurerBridge(originalMeasurer: () -> Long): () -> Long =
    { originalMeasurer() }

  override fun <T> createIterationMeasurer(
    instance: T,
    benchmark: BenchmarkDescriptor<T>,
    configuration: BenchmarkConfiguration,
    cycles: Int
  ): () -> Long {
    val measurer = super.createIterationMeasurer(instance, benchmark, configuration, cycles)
    return if (!configuration.enableJsBridge) measurer else createJsMeasurerBridge(measurer)
  }
}
