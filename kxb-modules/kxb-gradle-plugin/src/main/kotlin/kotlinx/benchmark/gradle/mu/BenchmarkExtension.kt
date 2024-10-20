package kotlinx.benchmark.gradle.mu

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.*
import kotlinx.benchmark.gradle.mu.internal.utils.adding
import kotlinx.benchmark.gradle.mu.tooling.JsToolsExtension
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register

abstract class BenchmarkExtension
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  objects: ObjectFactory,
) : ExtensionAware {

  abstract val baseGenerationDir: DirectoryProperty

  abstract val baseReportsDir: DirectoryProperty

  /**
   * Benchmark executions. Will be run once per [target][targets].
   */
  val benchmarkRuns: BenchmarkRunSpecsContainer =
    extensions.adding("benchmarkRuns", objects.benchmarkRunSpecsContainer())

  /**
   * Benchmark source targets. E.g. for JVM, JS, or custom.
   */
  val targets: BenchmarkTargetsContainer =
    extensions.adding("targets", objects.benchmarkTargetsContainer())

  val versions: Versions =
    extensions.adding("versions", objects.newInstance())

  val jsTools: JsToolsExtension =
    extensions.adding("tools", objects.newInstance<JsToolsExtension>())

  @KotlinxBenchmarkPluginInternalApi
  abstract val kotlinJsNodeModulesDir: DirectoryProperty

  @KotlinxBenchmarkPluginInternalApi
  abstract val enableDemoMode: Property<Boolean>

  abstract class Versions {
    abstract val benchmarksGenerator: Property<String>
    abstract val jmh: Property<String>

    abstract val benchmarkJs: Property<String>
    abstract val jsSourceMapSupport: Property<String>
  }

  fun BenchmarkTargetsContainer.registerJvm(
    name: String,
    configure: BenchmarkTarget.Kotlin.JVM.() -> Unit,
  ): NamedDomainObjectProvider<BenchmarkTarget.Kotlin.JVM> =
    register<BenchmarkTarget.Kotlin.JVM>(name, configure)

  fun BenchmarkTargetsContainer.registerJs(
    name: String,
    configure: BenchmarkTarget.Kotlin.JS.() -> Unit,
  ): NamedDomainObjectProvider<BenchmarkTarget.Kotlin.JS> =
    register<BenchmarkTarget.Kotlin.JS>(name, configure)

  fun BenchmarkTargetsContainer.registerWasmJs(
    name: String,
    configure: BenchmarkTarget.Kotlin.WasmJs.() -> Unit,
  ): NamedDomainObjectProvider<BenchmarkTarget.Kotlin.WasmJs> =
    register<BenchmarkTarget.Kotlin.WasmJs>(name, configure)

  fun BenchmarkTargetsContainer.registerNative(
    name: String,
    configure: BenchmarkTarget.Kotlin.Native.() -> Unit,
  ): NamedDomainObjectProvider<BenchmarkTarget.Kotlin.Native> =
    register<BenchmarkTarget.Kotlin.Native>(name, configure)

  companion object
}
