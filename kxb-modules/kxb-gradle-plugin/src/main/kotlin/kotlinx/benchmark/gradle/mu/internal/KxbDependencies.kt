package kotlinx.benchmark.gradle.mu.internal

import kotlinx.benchmark.gradle.mu.BenchmarkExtension
import kotlinx.benchmark.gradle.mu.internal.utils.declarable
import kotlinx.benchmark.gradle.mu.internal.utils.resolvable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named

/**
 * Utility for containing all Gradle [Configuration]s used by Benchmarks.
 */
internal class KxbDependencies(
  project: Project,
  benchmarksExtension: BenchmarkExtension,
) {
  private val objects: ObjectFactory = project.objects

  private val kxbGenerator: Configuration =
    project.configurations.create("kxbGenerator") {
      description =
        "Internal Kotlinx Benchmarks Configuration. Contains declared dependencies required for running benchmark generators."
      declarable()

      defaultDependencies {
        addLater(
          benchmarksExtension.versions.benchmarksGenerator.map { version ->
            project.dependencies.create(
              "dev.adamko.kotlinx-benchmark-mu:kxb-generator:$version"
            )
          }
        )
        addLater(
          benchmarksExtension.versions.benchmarksGenerator.map { version ->
            project.dependencies.create(
              "dev.adamko.kotlinx-benchmark-mu:kxb-runner-parameters:$version"
            )
          }
        )
//        addLater(
//          benchmarksExtension.versions.kotlinCompiler.map { version ->
//            project.dependencies.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:$version")
//          }
//        )
      }
    }

  val kxbGeneratorResolver: Configuration =
    project.configurations.createResolver("kxbGeneratorResolver") {
      description =
        "Internal Kotlinx Benchmarks Configuration. Resolves dependencies required for running benchmark generators."

      extendsFrom(kxbGenerator)

      attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
      }
    }

  private val kxbBenchmarkRunnerJvm: Configuration =
    project.configurations.create("kxbBenchmarkRunnerJvm") {
//      description =
//        "Internal Kotlinx Benchmarks Configuration. Contains declared dependencies required for running benchmark generators."
      declarable()

      defaultDependencies {
        addLater(
          benchmarksExtension.versions.benchmarksGenerator.map { version ->
            project.dependencies.create(
              "dev.adamko.kotlinx-benchmark-mu:kxb-runner:$version"
            )
          }
        )
        addLater(
          benchmarksExtension.versions.benchmarksGenerator.map { version ->
            project.dependencies.create(
              "dev.adamko.kotlinx-benchmark-mu:kxb-runner-parameters:$version"
            )
          }
        )
        addLater(
          benchmarksExtension.versions.benchmarksGenerator.map { version ->
            project.dependencies.create(
              "dev.adamko.kotlinx-benchmark-mu:kxb-generator:$version"
            )
          }
        )
      }
    }

  val kxbBenchmarkRunnerJvmResolver: Configuration =
    project.configurations.createResolver("kxbBenchmarkRunnerJvmResolver") {
//      description =
//        "Internal Kotlinx Benchmarks Configuration. Resolves dependencies required for running benchmark generators."

      extendsFrom(kxbBenchmarkRunnerJvm)

      attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
      }
    }

  companion object {
    private fun ConfigurationContainer.createResolver(
      name: String,
      configure: Configuration.() -> Unit
    ): Configuration {
      // The name has a tilde ~, so generated accessors require backticks to escape the name,
      // to discourage users from using it.
      return create("$name~internal") {
        resolvable()
        configure()
      }
    }
  }
}
