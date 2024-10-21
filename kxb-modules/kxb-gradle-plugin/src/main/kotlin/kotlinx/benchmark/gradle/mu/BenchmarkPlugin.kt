package kotlinx.benchmark.gradle.mu

import java.io.File
import javax.inject.Inject
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.BENCHMARK_PLUGIN_VERSION
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.JMH_DEFAULT_VERSION
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import kotlinx.benchmark.gradle.mu.config.tools.D8ToolSpec
import kotlinx.benchmark.gradle.mu.internal.KxbDependencies
import kotlinx.benchmark.gradle.mu.internal.KxbTasks
import kotlinx.benchmark.gradle.mu.internal.adapters.KxbJavaAdapter
import kotlinx.benchmark.gradle.mu.internal.adapters.KxbKotlinAdapter
import kotlinx.benchmark.gradle.mu.internal.fetchKotlinJsNodeModulesDir
import kotlinx.benchmark.gradle.mu.internal.utils.buildName
import kotlinx.benchmark.gradle.mu.internal.utils.toBoolean
import kotlinx.benchmark.gradle.mu.tasks.generate.BaseGenerateBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.run.*
import kotlinx.benchmark.gradle.mu.tasks.tools.D8SetupTask
import kotlinx.benchmark.gradle.mu.tasks.tools.NodeJsSetupTask
import kotlinx.benchmark.gradle.mu.tooling.Platform
import kotlinx.benchmark.gradle.mu.tooling.Platform.Arch.*
import kotlinx.benchmark.gradle.mu.tooling.Platform.System.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
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

    configureJsTools(project, kxbExtension)

    val kxbDependencies = KxbDependencies(project, kxbExtension)
    val kxbTasks = KxbTasks(project, kxbDependencies)

    configureKxbLifecycleTasks(project, kxbTasks)

    project.pluginManager.apply(KxbKotlinAdapter::class)
    project.pluginManager.apply(KxbJavaAdapter::class)

    configureAllOpenPlugin(project)

    configureGenerateBenchmarkTasks(project, kxbDependencies)
    configureRunBenchmarkTasks(project, kxbExtension)

    kxbExtension.targets.withType<BenchmarkTarget.KotlinJvm>().all {
      handleKotlinJvmTarget(project, kxbExtension, kxbDependencies, this)
    }
    kxbExtension.targets.withType<BenchmarkTarget.KotlinNative>().all {
      handleKotlinNativeTarget(project, kxbExtension, this)
    }
    kxbExtension.targets.withType<BenchmarkTarget.KotlinJs>().all {
      handleKotlinJsTarget(project, kxbExtension, kxbTasks, this)
    }
    kxbExtension.targets.withType<BenchmarkTarget.KotlinWasmJs>().all {
      handleKotlinWasmJsTarget(project, kxbExtension, kxbTasks, this)
    }
  }

  private fun createKxbExtension(project: Project): BenchmarkExtension {
    return project.extensions.create("benchmark", BenchmarkExtension::class).apply {

      benchmarkRuns.configureEach {
        enabled.convention(true)
        iterations.convention(1)
        warmupIterations.convention(0)
//        iterationDuration.convention(10.seconds)
        mode.convention(RunnerConfiguration.Mode.Throughput)
        resultFormat.convention(RunnerConfiguration.ResultFormat.Text)
        resultTimeUnit.convention(RunnerConfiguration.ReportTimeUnit.Seconds)

        excludes.set(emptySet())
        excludes.convention(emptySet())

        parameters.set(emptyMap<String, List<String>>())
        parameters.convention(emptyMap<String, List<String>>())

        advanced.set(emptyMap<String, String>())
        advanced.convention(emptyMap<String, String>())
        jvmBenchmarkLauncher.convention(this@apply.jvmBenchmarkLauncher)
      }

      enableDemoMode.convention(
        providers.gradleProperty("kotlinx.benchmark.demoMode").toBoolean()
      )

      versions.apply {
        jmh.convention(JMH_DEFAULT_VERSION)
        benchmarksRunner.convention(BENCHMARK_PLUGIN_VERSION)
        benchmarksGenerator.convention(BENCHMARK_PLUGIN_VERSION)
        benchmarkJs.convention("2.1.4")
        jsSourceMapSupport.convention("0.5.21")
      }

      kotlinJsNodeModulesDir.convention(fetchKotlinJsNodeModulesDir(project))

      configureBenchmarkTargetConventions()
    }
  }

  private fun BenchmarkExtension.configureBenchmarkTargetConventions() {
    targets.apply {
      registerBinding(BenchmarkTarget.KotlinJvm::class, BenchmarkTarget.KotlinJvm::class)
      registerBinding(BenchmarkTarget.KotlinJs::class, BenchmarkTarget.KotlinJs::class)
      registerBinding(BenchmarkTarget.KotlinNative::class, BenchmarkTarget.KotlinNative::class)
      registerBinding(BenchmarkTarget.KotlinWasmJs::class, BenchmarkTarget.KotlinWasmJs::class)
      registerBinding(BenchmarkTarget.KotlinWasmWasi::class, BenchmarkTarget.KotlinWasmWasi::class)
      registerBinding(BenchmarkTarget.Java::class, BenchmarkTarget.Java::class)

      configureEach {
        enabled.convention(false)
      }

      withType<BenchmarkTarget.KotlinJs>().configureEach {
        title.convention(name)
      }

      withType<BenchmarkTarget.KotlinWasmJs>().configureEach {
        title.convention(name)
      }
    }
  }

  private fun configureJsTools(
    project: Project,
    extension: BenchmarkExtension,
  ) {
    extension.jsTools.apply {

      hostPlatform.set(Platform.getHostPlatform(providers))
      toolsDir.convention(layout.buildDirectory.dir("kxb/tools"))

      d8.apply {
        edition.convention(D8ToolSpec.Edition.Debug)
        installationDir.convention(toolsDir.dir("d8"))
        platform.convention(hostPlatform.map { p ->
          buildString {
            when (p.system) {
              Linux   -> append("linux")
              MacOS   -> append("mac")
              Windows -> append("win")
              SunOS   -> error("D8 does not support system: ${p.system}")
            }
            when (p.arch) {
              Arm64 -> append("-arm64")
              X64   -> append("64")
              X86   -> append("86")
              Ppc64LE,
              S390x -> error("D8 does not support architecture: ${p.arch}")
            }
          }
        })
      }
      project.tasks.withType<D8SetupTask>().configureEach {
        this.installationDir.convention(extension.jsTools.d8.installationDir)
      }

      nodeJs.apply {
        command.convention("node")
        version.convention("22.0.0")
        installationDir.convention(toolsDir.dir("nodejs"))
//        downloadBaseUrl("https://nodejs.org/dist")
        distDownloadUrl.convention(
          hostPlatform.zip(version) { p, version ->
            val system = when (p.system) {
              Linux   -> "linux"
              MacOS   -> "darwin"
              Windows -> "win"
              SunOS   -> error("Unsupported Node.js platform: $p")
            }

            val arch = when (p.arch) {
              X64     -> "x64"
              Arm64   -> "arm64"
              Ppc64LE -> "ppc64le"
              S390x   -> "s390x"
              X86     -> "x86"
            }

            val ext = if (p.system == Windows) "zip" else "tar.gz"

            "https://nodejs.org/dist/v$version/node-v$version-${system}-$arch.$ext"
          }
        )
      }
    }

    project.tasks.withType<NodeJsSetupTask>().configureEach {
      this.installationDir.convention(extension.jsTools.nodeJs.installationDir)
      this.distDownloadUrl.convention(extension.jsTools.nodeJs.distDownloadUrl)
      this.cacheDir.set(temporaryDir.resolve("cache"))
    }
  }

  private fun configureKxbLifecycleTasks(
    project: Project,
    kxbTasks: KxbTasks,
  ) {
    kxbTasks.assembleBenchmarks.configure {
      dependsOn(project.tasks.withType<BaseGenerateBenchmarkTask>())
    }
    kxbTasks.benchmarks.configure {
      dependsOn(project.tasks.withType<BaseRunBenchmarksTask>())
      // Trick IntelliJ into thinking this is a test task,
      // so we can log test data via stdout encoded with IJ XML.
      extensions.extraProperties.set(
        "idea.internal.test",
        providers.systemProperty("idea.active").getOrElse("false").toBoolean(),
      )
    }
  }

  /**
   * Configure the Kotlin all-open plugin, so
   * [@State][org.openjdk.jmh.annotations.State]
   * annotated classes are automatically registered.
   */
  private fun configureAllOpenPlugin(project: Project) {
    project.pluginManager.withPlugin("org.jetbrains.kotlin.plugin.allopen") {
      project.extensions.configure<AllOpenExtension> {
        annotation("org.openjdk.jmh.annotations.State")
      }
    }
  }

  /**
   * Configure all tasks used to generate code used to run the benchmarks.
   */
  private fun configureGenerateBenchmarkTasks(
    project: Project,
    kxbDependencies: KxbDependencies,
  ) {
    project.tasks.withType<BaseGenerateBenchmarkTask>().configureEach {
      runtimeClasspath.from(kxbDependencies.kxbGeneratorResolver)
      generatedResources.convention(temporaryDir.resolve("generated-resources"))
      generatedSources.convention(temporaryDir.resolve("generated-sources"))
    }
  }

  /**
   * Configure all tasks used to run benchmarks.
   *
   * @see BaseRunBenchmarksTask
   */
  private fun configureRunBenchmarkTasks(
    project: Project,
    kxbExtension: BenchmarkExtension,
  ) {
    project.tasks.withType<BaseRunBenchmarksTask>().configureEach {

      onlyIf("BenchmarkRunSpec.enabled") {
        require(it is BaseRunBenchmarksTask)
        it.benchmarkParameters.get().enabled.getOrElse(true)
      }

      enableDemoMode.convention(kxbExtension.enableDemoMode)
      ideaActive.convention(providers.systemProperty("idea.active").toBoolean())

      // Trick IntelliJ into thinking this is a test task,
      // so we can log test data via stdout encoded with IJ XML.
      extensions.extraProperties.set(
        "idea.internal.test",
        providers.systemProperty("idea.active").getOrElse("false").toBoolean(),
      )

      report.convention(
        benchmarkParameters.flatMap { it.resultFormat }
          .map { format -> "result.${format.extension}" }
          .map { filename -> temporaryDir.resolve("output/$filename") }
      )
    }

    project.tasks.withType<RunJvmBenchmarkTask>().configureEach {
      mainClass.convention("kotlinx.benchmark.jvm.JvmBenchmarkRunnerKt")
      ignoreJmhLock.convention(false)
    }

    project.tasks.withType<RunNativeBenchmarkTask>().configureEach {
      workingDir.convention(temporaryDir.resolve("work"))
      benchmarkDescriptionDir.convention(temporaryDir.resolve("descriptors"))
      benchmarkProgress.convention(temporaryDir.resolve("progress.txt"))
      report.convention(temporaryDir.resolve("report.txt"))
    }

    project.tasks.withType<RunJsNodeBenchmarkTask>().configureEach {
      sourceMapStackTraces.convention(true)
      cacheDir.convention(temporaryDir.resolve("cache"))
    }

    project.tasks.withType<RunJsD8BenchmarkTask>().configureEach {
      workingDir.convention(temporaryDir.resolve("work"))
    }
  }

  private fun handleKotlinJvmTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbDependencies: KxbDependencies,
    target: BenchmarkTarget.KotlinJvm,
  ) {
    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunJvmBenchmarkTask>(
        name = buildName("benchmark", target.name, runSpec.name)
      ) {
        description = "Executes benchmark for JVM target ${target.name}"

        this.javaLauncher.convention(runSpec.jvmBenchmarkLauncher)

        runtimeClasspath.from(target.jarTask)
        runtimeClasspath.from(target.runtimeClasspath)
        runtimeClasspath.from(target.targetRuntimeDependencies)
        runtimeClasspath.from(target.compiledTarget)
        runtimeClasspath.from(kxbDependencies.kxbBenchmarkRunnerJvmResolver)
        benchmarkParameters.set(runSpec)
      }
    }
  }

  private fun handleKotlinNativeTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    target: BenchmarkTarget.KotlinNative,
  ) {

    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunNativeBenchmarkTask>(
        name = buildName("benchmark", target.name, runSpec.name)
      ) {
        description = "Executes benchmark for JS target ${target.name} using NodeJS"

        this.benchmarkParameters.set(runSpec)
        this.executable.convention(target.executable)

        this.forkMode.convention(target.forkMode)
      }
    }
  }

  private fun handleKotlinJsTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbTasks: KxbTasks,
    target: BenchmarkTarget.KotlinJs,
  ) {
    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunJsNodeBenchmarkTask>(
        name = buildName("benchmark", target.name, runSpec.name)
      ) {
        description = "Executes benchmark for JS target ${target.name} using NodeJS"

        benchmarkParameters.set(runSpec)

        module.convention(target.compiledExecutableModule)

        nodeExecutable.convention(
          kxbTasks.setupNodeJsBenchmarkRunner.map { it.installationDir.get().file("bin/node") }
        )

        requiredJsFiles.from(target.requiredJsFiles)
      }
    }
  }

  private fun handleKotlinWasmJsTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbTasks: KxbTasks,
    target: BenchmarkTarget.KotlinWasmJs,
  ) {
    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunJsNodeBenchmarkTask>(
        name = buildName("benchmark", target.name, runSpec.name)
      ) {
        description = "Executes benchmark for WasmJS target ${target.name} using NodeJS"

        benchmarkParameters.set(runSpec)

        module.convention(target.compiledExecutableModule)

        nodeExecutable.convention(
          kxbTasks.setupNodeJsBenchmarkRunner.map { it.installationDir.get().file("bin/node") }
        )

        requiredJsFiles.from(target.requiredJsFiles)
      }
    }
  }


  //region workaround for https://github.com/gradle/gradle/issues/23708
  private fun RegularFileProperty.convention(value: File): RegularFileProperty =
    convention(objects.fileProperty().fileValue(value))

  private fun RegularFileProperty.convention(value: Provider<File>): RegularFileProperty =
    convention(layout.file(value))

  private fun DirectoryProperty.convention(value: File): DirectoryProperty =
    convention(objects.directoryProperty().fileValue(value))

  private fun DirectoryProperty.convention(value: Provider<File>): DirectoryProperty =
    convention(layout.dir(value))
  //endregion
}
