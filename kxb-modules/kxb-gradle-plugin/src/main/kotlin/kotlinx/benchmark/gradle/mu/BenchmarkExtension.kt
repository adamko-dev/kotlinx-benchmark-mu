package kotlinx.benchmark.gradle.mu

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkTargetsContainer
import kotlinx.benchmark.gradle.mu.config.*
import kotlinx.benchmark.gradle.mu.config.benchmarkTargetsContainer
import kotlinx.benchmark.gradle.mu.internal.utils.adding
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance

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
    extensions.adding("targets",
      objects.benchmarkTargetsContainer()
    )

  val versions: Versions = extensions.adding("versions", objects.newInstance())

  abstract val enableDemoMode: Property<Boolean>

  abstract class Versions {
    abstract val benchmarksGenerator: Property<String>
    abstract val jmh: Property<String>
  }

  companion object
}
