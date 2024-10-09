package kotlinx.benchmark.gradle.mu

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
import kotlinx.benchmark.gradle.mu.internal.utils.toBoolean
import kotlinx.benchmark.gradle.mu.internal.utils.uppercaseFirstChar
import kotlinx.benchmark.gradle.mu.tasks.*
import kotlinx.benchmark.gradle.mu.tasks.tools.D8SetupTask
import kotlinx.benchmark.gradle.mu.tasks.tools.NodeJsSetupTask
import kotlinx.benchmark.gradle.mu.tooling.Platform
import kotlinx.benchmark.gradle.mu.tooling.Platform.Arch.*
import kotlinx.benchmark.gradle.mu.tooling.Platform.System.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

abstract class BenchmarkPlugin
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
  private val javaToolchains: JavaToolchainService,
) : Plugin<Project> {

  override fun apply(project: Project) {
    val kxbExtension = createKxbExtension(project)

    configureJsTools(project, kxbExtension)

    val kxbDependencies = KxbDependencies(project, kxbExtension)
    val kxbTasks = KxbTasks(project, kxbDependencies)

    project.pluginManager.apply(KxbKotlinAdapter::class)
    project.pluginManager.apply(KxbJavaAdapter::class)

    configureAllOpenPlugin(project)

    configureKxbTasks(project, kxbExtension, kxbDependencies)

    kxbExtension.targets.withType<BenchmarkTarget.Kotlin.JVM>().all {
      handleKotlinJvmTarget(project, kxbExtension, kxbDependencies, this)
    }
    kxbExtension.targets.withType<BenchmarkTarget.Kotlin.JS>().all {
      handleKotlinJsTarget(project, kxbExtension, kxbDependencies, kxbTasks, this)
    }
  }

  private fun createKxbExtension(project: Project): BenchmarkExtension {
    return project.extensions.create("benchmark", BenchmarkExtension::class).apply {

      benchmarkRuns.configureEach {
        enabled.convention(true)
        iterations.convention(1)
        warmups.convention(0)
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
      }

      enableDemoMode.convention(
        providers.gradleProperty("kotlinx.benchmark.demoMode").map { it.toBoolean() }
      )

      targets.apply {
        registerFactory(BenchmarkTarget.Kotlin.JVM::class.java) { name ->
          objects.newInstance(name, project)
        }

        registerBinding(BenchmarkTarget.Kotlin.JS::class, BenchmarkTarget.Kotlin.JS::class)
        registerBinding(BenchmarkTarget.Kotlin.Native::class, BenchmarkTarget.Kotlin.Native::class)
        registerBinding(BenchmarkTarget.Kotlin.WasmJs::class, BenchmarkTarget.Kotlin.WasmJs::class)
        registerBinding(BenchmarkTarget.Kotlin.WasmWasi::class, BenchmarkTarget.Kotlin.WasmWasi::class)
        registerBinding(BenchmarkTarget.Java::class, BenchmarkTarget.Java::class)
      }

      targets.configureEach {
        enabled.convention(false)
      }

      versions.apply {
        jmh.convention(JMH_DEFAULT_VERSION)
        benchmarksGenerator.convention(BENCHMARK_PLUGIN_VERSION)
        benchmarkJs.convention("2.1.4")
        jsSourceMapSupport.convention("0.5.21")
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
              SunOS   -> error("Unsupported platform: $p")
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
//      this.arch.convention(extension.jsTools.hostPlatform.map { it.arch })
      this.distDownloadUrl.convention(extension.jsTools.nodeJs.distDownloadUrl)
      this.cacheDir.set(temporaryDir.resolve("cache"))
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
    project.tasks.withType<JsSourceGeneratorTask>().configureEach {
      runtimeClasspath.from(kxbDependencies.kxbGeneratorResolver)
      generatedResources.convention(
        objects.directoryProperty().fileValue(temporaryDir.resolve("generated-resources"))
      )
      generatedSources.convention(
        objects.directoryProperty().fileValue(temporaryDir.resolve("generated-sources"))
      )
    }

    project.tasks.withType<RunBenchmarkBaseTask>().configureEach {
      enableDemoMode.convention(kxbExtension.enableDemoMode)
      ideaActive.convention(providers.systemProperty("idea.active").toBoolean())
    }

    project.tasks.withType<RunJvmBenchmarkTask>().configureEach {
      mainClass.convention("kotlinx.benchmark.jvm.JvmBenchmarkRunnerKt")

      // TODO inherit javaLauncher from Kotlin/Java plugins
      javaLauncher.convention(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) })
    }

    project.tasks.withType<RunJsNodeBenchmarkTask>().configureEach {
      workingDir.fileValue(temporaryDir)
      sourceMapStackTraces.convention(true)
    }
    project.tasks.withType<RunJsD8BenchmarkTask>().configureEach {
      workingDir.fileValue(temporaryDir)
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
        description = "Executes benchmark for JVM target ${target.name}"

        runtimeClasspath.from(target.jarTask)
        runtimeClasspath.from(target.runtimeClasspath)
        runtimeClasspath.from(target.targetRuntimeDependencies)
        runtimeClasspath.from(target.compiledTarget)
        runtimeClasspath.from(kxbDependencies.kxbBenchmarkRunnerJvmResolver)
        benchmarkParameters.set(runSpec)
      }
    }
  }

  private fun handleKotlinJsTarget(
    project: Project,
    kxbExtension: BenchmarkExtension,
    kxbDependencies: KxbDependencies,
    kxbTasks: KxbTasks,
    target: BenchmarkTarget.Kotlin.JS,
  ) {
    kxbExtension.benchmarkRuns.all {
      val runSpec = this
      project.tasks.register<RunJsNodeBenchmarkTask>("benchmark${target.name.uppercaseFirstChar()}${runSpec.name.uppercaseFirstChar()}") {
        description = "Executes benchmark for JS target ${target.name} using NodeJS"

        benchmarkParameters.set(runSpec)

        runArguments.convention(
          listOf(
            "-r",
            "source-map-support/register",
          )
        )

//        module.convention(target.compiledExecutableModule)
        module.from(target.compiledExecutableModule)

        nodeExecutable.convention(
          kxbTasks.setupNodeJsBenchmarkRunner.map { it.installationDir.get().file("bin/node") }
        )
      }
    }
  }
}
