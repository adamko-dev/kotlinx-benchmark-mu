package kotlinx.benchmark.gradle.mu.config

import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.Named
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
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
  abstract val warmups: Property<Int>

  @get:Input
  @get:Optional
  abstract val iterationDuration: Property<Duration>

  @get:Input
  @get:Optional
  abstract val mode: Property<BenchmarkMode>

  @get:Input
  abstract val reportFormat: Property<ReportFormat>

  @get:Input
  @get:Optional
  abstract val reportTimeUnit: Property<ReportTimeUnit>

  @get:Input
  @get:Optional
  abstract val includes: SetProperty<String>

  @get:Input
  @get:Optional
  abstract val excludes: SetProperty<String>

  // todo typesafe params...
  @get:Input
  @get:Optional
  abstract val params: MapProperty<String, List<String>>
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


  fun iterationDuration(duration: java.time.Duration) {
    iterationDuration.set(duration.toKotlinDuration())
  }

  fun iterationDuration(duration: Provider<java.time.Duration>) {
    iterationDuration.set(duration.map { it.toKotlinDuration() })
  }

  fun iterationDuration(duration: Duration) {
    iterationDuration.set(duration)
  }

  @JvmSynthetic
  @JvmName("iterationDurationKt")
  fun iterationDuration(duration: Provider<Duration>) {
    iterationDuration.set(duration.map { it })
  }
}
