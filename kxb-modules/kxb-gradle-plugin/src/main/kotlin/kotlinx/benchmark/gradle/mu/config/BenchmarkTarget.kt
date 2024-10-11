package kotlinx.benchmark.gradle.mu.config

import javax.inject.Inject
import kotlin.collections.set
import kotlinx.benchmark.gradle.BenchmarksPlugin
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.buildName
import kotlinx.benchmark.gradle.mu.tasks.GenerateJvmBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.JsSourceGeneratorTask
import kotlinx.benchmark.gradle.mu.tasks.NativeSourceGeneratorTask
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
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

    abstract class JS
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      @get:Input
      final override val targetName: String,
      project: Project,
//      internal val compilationTask: TaskProvider<Task>,
//      internal val linkTask: TaskProvider<Task>,
//      internal val linkSyncTask: TaskProvider<Task>,
    ) : Kotlin(targetName) {
      @get:Input
      abstract val title: Property<String>

      //      abstract val compiledExecutableModule: RegularFileProperty
      abstract val compiledExecutableModule: ConfigurableFileCollection

//      abstract val runner: Property<JsRunner>
//
//      enum class JsRunner {
////        D8,
//        NodeJs,
//      }

      @get:Input
      abstract val benchmarksExecutor: Property<JsBenchmarksExecutor>

      abstract val requiredJsFiles: ConfigurableFileCollection

      //    val benchmarkBuildDir = benchmarkBuildDir(target)
      val generatorTask =
        project.tasks.register<JsSourceGeneratorTask>(
          buildName("kxb", "Generate", targetName, "Benchmarks")
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Generate sources for running Kotlin/JS '${targetName}' benchmarks."
          title.convention(targetName)
          benchmarksExecutor.convention(JsBenchmarksExecutor.BenchmarkJs)
//        title = target.name
//        useBenchmarkJs = target.jsBenchmarksExecutor == JsBenchmarksExecutor.BenchmarkJs
//        inputClassesDirs = compilationOutput.output.allOutputs
//        inputDependencies = compilationOutput.compileDependencyFiles
//        outputResourcesDir = file("$benchmarkBuildDir/resources")
//        outputSourcesDir = file("$benchmarkBuildDir/sources")
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
//      abstract val executable: ConfigurableFileCollection

      enum class ForkMode {
        PerTest,
        PerBenchmark
      }

      val generatorTask =
        project.tasks.register<NativeSourceGeneratorTask>(
          buildName("kxbGenerate", targetName, "Benchmarks")
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Generate benchmark sources for Kotlin/Native target '${targetName}'"
          this.targetName.convention(this@Native.targetName)
          this.title.convention(this@Native.title)
        }
    }

    abstract class WasmWasi
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin(targetName) {

      enum class WasmRunner {
        D8,
        NodeJs,
      }
    }

    abstract class WasmJs
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin(targetName) {

      enum class WasmRunner {
        D8,
        NodeJs,
      }
    }
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
