package kotlinx.benchmark.gradle.mu.config

import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.DurationJdk
import org.gradle.api.Named
import org.gradle.api.provider.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class BenchmarkRunSpec
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(private val name: String) : Named {

  @get:Input
  @get:Optional
  abstract val enabled: Property<Boolean> // TODO enable/disable BenchmarkRun

  @get:Input
  @get:Optional
  abstract val iterations: Property<Int>
  @get:Input
  @get:Optional
  abstract val forks: Property<Int>
  @get:Input
  @get:Optional
  abstract val warmupIterations: Property<Int>
  @get:Input
  @get:Optional
  abstract val threads: Property<Int>

  @get:Input
  @get:Optional
  abstract val failOnError: Property<Boolean>
  @get:Input
  @get:Optional
  abstract val gcEachIteration: Property<Boolean>
  @get:Input
  @get:Optional
  abstract val synchronizeIterations: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val warmups: Property<Int>

  @get:Input
  @get:Optional
  abstract val warmupForks: Property<Int>

  @get:Input
  @get:Optional
  abstract val timeout: Property<Duration>

  @get:Input
  @get:Optional
  abstract val warmupTime: Property<Duration>

  @get:Internal // Cannot fingerprint input property 'iterationDuration': value '300ms' cannot be serialized.
  abstract val iterationDuration: Property<Duration>

  @get:Input
  @get:Optional
  @get:JvmSynthetic
  @KotlinxBenchmarkPluginInternalApi
  @Deprecated(level = DeprecationLevel.HIDDEN, message = "Because Gradle can't serializer kotlin.time.Duration")
  @Suppress("unused")
  protected val iterationDurationProvider: Provider<DurationJdk>
    get() = iterationDuration.map { it.toJavaDuration() }

  @get:Input
  @get:Optional
  abstract val mode: Property<RunnerConfiguration.Mode>

  @get:Input
  abstract val resultFormat: Property<RunnerConfiguration.ResultFormat>

  @get:Input
  @get:Optional
  abstract val resultTimeUnit: Property<RunnerConfiguration.ReportTimeUnit>

  @get:Input
  @get:Optional
  abstract val includes: SetProperty<String>

  @get:Input
  @get:Optional
  abstract val excludes: SetProperty<String>
  @get:Input
  @get:Optional
  abstract val profilers: SetProperty<String>
  @get:Input
  @get:Optional
  abstract val jvmArgs: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val warmupBenchmarks: SetProperty<String>

  // todo typesafe params...
  @get:Input
  @get:Optional
  abstract val parameters: MapProperty<String, List<String>>
  @get:Input

  // todo typesafe advanced...
  @get:Optional
  abstract val advanced: MapProperty<String, String>

  fun include(pattern: String) {
    includes.add(pattern)
  }

  fun exclude(pattern: String) {
    excludes.add(pattern)
  }

//  fun param(name: String, vararg value: Any?) {
//    val values = params.getOrPut(name) { mutableListOf() }
//    values.addAll(value)
//  }
//
//  fun advanced(name: String, value: Any?) {
//    advanced[name] = value
//  }
//
//  @KotlinxBenchmarkPluginInternalApi
//  fun capitalizedName() = if (name == "main") "" else name.capitalize()
//
//  @KotlinxBenchmarkPluginInternalApi
//  fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.capitalize()
//
//  @KotlinxBenchmarkPluginInternalApi
//  fun reportFileExt(): String = reportFormat?.toLowerCase() ?: "json"

  @Input
  override fun getName(): String = name


  fun iterationDuration(duration: DurationJdk) {
    iterationDuration.set(duration.toKotlinDuration())
  }

  fun iterationDuration(duration: Provider<DurationJdk>) {
    iterationDuration.set(duration.map { it.toKotlinDuration() })
  }

  fun iterationDuration(duration: Duration) {
    iterationDuration.set(duration)
  }

  //  @JvmSynthetic
  @JvmName("iterationDurationKt")
  fun iterationDuration(duration: Provider<Duration>) {
    iterationDuration.set(duration.map { it })
  }
}
