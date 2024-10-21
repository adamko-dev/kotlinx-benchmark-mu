package kotlinx.benchmark.gradle.mu.tasks.run

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec.Companion.buildRunnerConfig
import kotlinx.benchmark.gradle.mu.tasks.BaseBenchmarkTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault(because = "wip")
abstract class BaseRunBenchmarksTask
@KotlinxBenchmarkPluginInternalApi
protected constructor() : BaseBenchmarkTask() {

  @KotlinxBenchmarkPluginInternalApi
  @get:Console // only affects stdout logging
  abstract val ideaActive: Property<Boolean>

  @KotlinxBenchmarkPluginInternalApi
  @get:Input
  @get:Optional
  abstract val enableDemoMode: Property<Boolean>

  @get:OutputFile
  abstract val report: RegularFileProperty

  @get:Nested
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>

  protected fun encodeBenchmarkParameters(): String {
    val benchmarkParameters = benchmarkParameters.get()

    val reportFile = report.get().asFile.apply {
      parentFile.mkdirs()
    }

    val runnerConfig = buildRunnerConfig(
      name = benchmarkParameters.name,
      reportFile = reportFile,
      config = benchmarkParameters,
      reporting = if (ideaActive.getOrElse(false)) ProgressReporting.IntelliJ else ProgressReporting.Stdout
    )

    @OptIn(ExperimentalEncodingApi::class)
    return Base64.encode(runnerConfig.encodeToByteArray())
  }
}
