package kotlinx.benchmark.gradle.mu

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.BENCHMARK_PLUGIN_VERSION
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.JMH_DEFAULT_VERSION
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkMode
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import kotlinx.benchmark.gradle.mu.config.ReportFormat
import kotlinx.benchmark.gradle.mu.config.ReportTimeUnit
import kotlinx.benchmark.gradle.mu.internal.KxbDependencies
import kotlinx.benchmark.gradle.mu.internal.adapters.KxbJavaAdapter
import kotlinx.benchmark.gradle.mu.internal.adapters.KxbKotlinAdapter
import kotlinx.benchmark.gradle.mu.internal.utils.uppercaseFirstChar
import kotlinx.benchmark.gradle.mu.tasks.GenerateJvmBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.RunJvmBenchmarkTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

abstract class BenchmarkPlugin
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
) : Plugin<Project> {

  override fun apply(project: Project) {
    val kxbExtension = createKxbExtension(project)

    val kxbDependencies = KxbDependencies(project, kxbExtension)

    project.pluginManager.apply(KxbKotlinAdapter::class)
    project.pluginManager.apply(KxbJavaAdapter::class)

    configureAllOpenPlugin(project)

    configureKxbTasks(project, kxbExtension, kxbDependencies)

    kxbExtension.targets.withType<BenchmarkTarget.Kotlin.JVM>().all {
      handleKotlinJvmTarget(project, kxbExtension, kxbDependencies, this)
    }
  }

  private fun createKxbExtension(project: Project): BenchmarkExtension {
    return project.extensions.create("benchmark", BenchmarkExtension::class).apply {

      benchmarkRuns.configureEach {
        enabled.convention(true)
        iterations.convention(1)
        warmups.convention(0)
//        iterationDuration.convention(10.seconds)
        mode.convention(BenchmarkMode.Throughput)
        reportFormat.convention(ReportFormat.Text)
        reportTimeUnit.convention(ReportTimeUnit.Seconds)

        excludes.set(emptySet())
        excludes.convention(emptySet())

        params.set(emptyMap<String, List<String>>())
        params.convention(emptyMap<String, List<String>>())

        advanced.set(emptyMap<String, String>())
        advanced.convention(emptyMap<String, String>())
      }

      targets.apply {
        registerFactory(BenchmarkTarget.Kotlin.JVM::class.java) { name ->
          objects.newInstance(name, project)
        }

        registerBinding(BenchmarkTarget.Kotlin.JS::class, BenchmarkTarget.Kotlin.JS::class)
        registerBinding(BenchmarkTarget.Kotlin.Native::class, BenchmarkTarget.Kotlin.Native::class)
        registerBinding(BenchmarkTarget.Kotlin.Wasm::class, BenchmarkTarget.Kotlin.Wasm::class)
        registerBinding(BenchmarkTarget.Java::class, BenchmarkTarget.Java::class)
      }

      targets.configureEach {
        enabled.convention(false)
      }

      versions.jmh.convention(JMH_DEFAULT_VERSION)
      versions.benchmarksGenerator.convention(BENCHMARK_PLUGIN_VERSION)
    }
  }

  private fun configureAllOpenPlugin(project: Project) {
    project.pluginManager.withPlugin("org.jetbrains.kotlin.plugin.allopen") {
      project.extensions.configure<AllOpenExtension> {
        annotation("org.openjdk.jmh.annotations.State")
      }
    }
  }

  private fun configureKxbTasks(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbDependencies: KxbDependencies,
  ) {
    project.tasks.withType<GenerateJvmBenchmarkTask>().configureEach {
      runtimeClasspath.from(kxbDependencies.kxbGeneratorResolver)
      generatedResources.convention(
        objects.directoryProperty().fileValue(temporaryDir.resolve("generated-resources"))
      )
      generatedSources.convention(
        objects.directoryProperty().fileValue(temporaryDir.resolve("generated-sources"))
      )
    }

    project.tasks.withType<RunJvmBenchmarkTask>().configureEach {
      mainClass.convention("kotlinx.benchmark.jvm.JvmBenchmarkRunnerKt")
      ideaActive.convention(providers.systemProperty("idea.active").map { it.toBoolean() })
    }
  }

  private fun handleKotlinJvmTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbDependencies: KxbDependencies,
    target: BenchmarkTarget.Kotlin.JVM,
  ) {
    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunJvmBenchmarkTask>("benchmark${target.name.uppercaseFirstChar()}${runSpec.name.uppercaseFirstChar()}") {
        runtimeClasspath.from(target.jarTask)
        runtimeClasspath.from(target.runtimeClasspath)
        runtimeClasspath.from(target.targetRuntimeDependencies)
        runtimeClasspath.from(target.compiledTarget)
        runtimeClasspath.from(kxbDependencies.kxbBenchmarkRunnerJvmResolver)
        benchmarkParameters.set(runSpec)
      }
    }
  }
}
