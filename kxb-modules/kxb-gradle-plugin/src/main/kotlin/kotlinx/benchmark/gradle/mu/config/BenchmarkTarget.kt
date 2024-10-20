package kotlinx.benchmark.gradle.mu.config

import javax.inject.Inject
import kotlin.collections.set
import kotlinx.benchmark.gradle.BenchmarksPlugin
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.buildName
import kotlinx.benchmark.gradle.mu.tasks.generate.GenerateJsBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.generate.GenerateJvmBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.generate.GenerateNativeBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.generate.GenerateWasmBenchmarkTask
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation


sealed class BenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  private val name: String
) : Named {
  @get:Input
  abstract val enabled: Property<Boolean>

  @Input
  override fun getName(): String = name

  abstract class Java
  @KotlinxBenchmarkPluginInternalApi
  @Inject
  constructor(
    sourceSetName: String
  ) : BenchmarkTarget("java$sourceSetName")

  sealed class Kotlin
  @KotlinxBenchmarkPluginInternalApi
  @Inject
  constructor(
    named: String,
  ) : BenchmarkTarget(named) {
    @get:Input
    abstract val targetName: String

    abstract class JVM
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      @get:Input
      final override val targetName: String,
      project: Project,
    ) : Kotlin(targetName) {
      @get:Classpath
      abstract val compiledTarget: ConfigurableFileCollection

      @get:Classpath
      abstract val targetRuntimeDependencies: ConfigurableFileCollection

      @get:Classpath
      abstract val targetCompilationDependencies: ConfigurableFileCollection

      @get:Classpath
      abstract val runtimeClasspath: ConfigurableFileCollection

      val generateBenchmarkTask: TaskProvider<GenerateJvmBenchmarkTask> =
        project.tasks.register<GenerateJvmBenchmarkTask>(
          buildName("kxbGenerate", targetName)
        ) {
          description = "Generate JVM source files for '${targetName}'"
        }

      val compileTask: TaskProvider<JavaCompile> =
        project.tasks.register<JavaCompile>(
          buildName("kxbCompile", targetName)
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Compile JMH source files for '${targetName}'"
          dependsOn(generateBenchmarkTask)
          classpath = project.objects.fileCollection().apply {
            from(compiledTarget)
            from(targetCompilationDependencies)
          }
          source(generateBenchmarkTask.map { it.generatedSources })
          destinationDirectory.convention(
            project.objects.directoryProperty().fileValue(temporaryDir)
          )
        }

      val jarTask: TaskProvider<Jar> =
        project.tasks.register<Jar>(
          buildName("kxbJar", targetName)
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Build JAR for JMH compiled files for '${targetName}'"

          isZip64 = true
          archiveClassifier = "JMH"
          manifest {
            attributes["Main-Class"] = "org.openjdk.jmh.Main"
          }
          duplicatesStrategy = DuplicatesStrategy.FAIL

          from(compileTask) {
            include("**/*.class")
          }
          from(generateBenchmarkTask.map { it.generatedResources })

          destinationDirectory = temporaryDir
          archiveBaseName.set("${project.name}-${targetName}-jmh")
        }
    }

    abstract class Native
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String,
      project: Project,
    ) : Kotlin(targetName) {
      abstract val title: Property<String>

      abstract val forkMode: Property<ForkMode>

      abstract val executable: RegularFileProperty

      enum class ForkMode {
        PerTest,
        PerBenchmark
      }

      val generatorTask: TaskProvider<GenerateNativeBenchmarkTask> =
        project.tasks.register<GenerateNativeBenchmarkTask>(
          buildName("kxbGenerate", targetName, "Benchmarks")
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Generate benchmark sources for Kotlin/Native target '${targetName}'"
          this.targetName.convention(this@Native.targetName)
          this.title.convention(this@Native.title)
        }
    }

    abstract class JS
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String,
      project: Project,
    ) : Kotlin(targetName) {
      abstract val title: Property<String>

      abstract val compiledExecutableModule: RegularFileProperty

      @Deprecated("KxB only supports Node.js for JS benchmarks.")
      @Suppress("unused")
      abstract val engine: Property<JsEngine>

      abstract val benchmarksExecutor: Property<JsBenchmarksExecutor>

      /**
       * `--require` files
       */
      abstract val requiredJsFiles: ConfigurableFileCollection

      val generatorTask: TaskProvider<GenerateJsBenchmarkTask> =
        project.tasks.register<GenerateJsBenchmarkTask>(
          buildName("kxb", "Generate", targetName, "Benchmarks")
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Generate sources for running Kotlin/JS '${targetName}' benchmarks."
          title.convention(targetName)
          benchmarksExecutor.convention(JsBenchmarksExecutor.BenchmarkJs)
        }
    }

    abstract class WasmJs
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String,
      project: Project,
    ) : Kotlin(targetName) {
      abstract val title: Property<String>

      abstract val compiledExecutableModule: RegularFileProperty

      abstract val requiredJsFiles: ConfigurableFileCollection

      abstract val engine: Property<JsEngine>

      val generatorTask: TaskProvider<GenerateWasmBenchmarkTask> =
        project.tasks.register<GenerateWasmBenchmarkTask>(
          buildName("kxb", "Generate", targetName, "Benchmarks")
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Generate sources for running Kotlin/WasmJS '${targetName}' benchmarks."
          title.convention(this@WasmJs.title)
        }
    }

    abstract class WasmWasi
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin(targetName)
  }
}


abstract class JvmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String
) : BenchmarkTarget(name) {
//  var jmhVersion: String = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
}

abstract class JavaBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String,
  @property:KotlinxBenchmarkPluginInternalApi
  val sourceSet: SourceSet
) : JvmBenchmarkTarget(name)

abstract class KotlinJvmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String,
  @property:KotlinxBenchmarkPluginInternalApi
  val compilation: KotlinJvmCompilation
) : JvmBenchmarkTarget(name)


abstract class JsBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String,
  @property:KotlinxBenchmarkPluginInternalApi
  val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(name) {
  var jsBenchmarksExecutor: JsBenchmarksExecutor = JsBenchmarksExecutor.BenchmarkJs
}

abstract class WasmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String,
  @property:KotlinxBenchmarkPluginInternalApi
  val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(name)

abstract class NativeBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
  name: String,
  @property:KotlinxBenchmarkPluginInternalApi
  val compilation: KotlinNativeCompilation
) : BenchmarkTarget(name) {
  var buildType: NativeBuildType = NativeBuildType.RELEASE
}
