package kotlinx.benchmark.gradle.mu

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.*
import kotlinx.benchmark.gradle.mu.internal.utils.adding
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

  val benchmarkRuns: BenchmarkRunSpecsContainer =
    extensions.adding("benchmarkRuns", objects.benchmarkRunSpecsContainer())

  val targets: BenchmarkTargetsContainer =
    extensions.adding(
      "targets",
      objects.benchmarkTargetsContainer()
    )

  val versions: Versions = extensions.adding("versions", objects.newInstance())

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

  companion object
}
