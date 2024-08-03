package kotlinx.benchmark.gradle.mu.config

import javax.inject.Inject
import kotlin.collections.set
import kotlinx.benchmark.gradle.BenchmarksPlugin
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.tasks.GenerateJvmBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.RunJvmBenchmarkTask
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
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
  abstract val enabled: Property<Boolean>
//  var workingDir: String? = null

//  internal abstract val named: String

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
    abstract val targetName: String

    abstract class JVM
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String,
      project: Project,
    ) : Kotlin("kotlin$targetName") {
      @get:Classpath
      abstract val inputClasses: ConfigurableFileCollection

      @get:Classpath
      abstract val compileClasspath: ConfigurableFileCollection

      @get:Classpath
      abstract val runtimeClasspath: ConfigurableFileCollection
//      @get:OutputDirectory
//      abstract val outputResourcesDir: DirectoryProperty
//
//      @get:OutputDirectory
//      abstract val outputSourcesDir: DirectoryProperty


      val generateBenchmarkTask: TaskProvider<GenerateJvmBenchmarkTask> =
        project.tasks.register<GenerateJvmBenchmarkTask>("kxbGenerate${targetName}") {
          description = "Generate JMH source files for '${targetName}'"
//          dependsOn(compilationTask)
//        runtimeClasspath = workerClasspath
//        inputCompileClasspath = compileClasspath
//        inputClassesDirs = compilationOutput
//        outputResourcesDir = file("$benchmarkBuildDir/resources")
//        outputSourcesDir = file("$benchmarkBuildDir/sources")
//        executableProvider = javaLauncherProvider().map {
//            it.executablePath.asFile.absolutePath
//        }
        }

      val prepareResourcesTask: TaskProvider<Sync> =
        project.tasks.register<Sync>(
          "kxbPrepareResources${targetName}",
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          from(generateBenchmarkTask.map { it.generatedResources })
          into(temporaryDir)
        }

      val compileTask: TaskProvider<JavaCompile> =
        project.tasks.register<JavaCompile>(
          "kxbCompile${targetName}",
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Compile JMH source files for '${targetName}'"
          dependsOn(generateBenchmarkTask)
          classpath = compileClasspath
//          classpath = project.objects.fileCollection().apply {
//            from(compileClasspath)
//            from()
//          }
          source(generateBenchmarkTask.map { it.generatedSources })
//        source = fileTree("$benchmarkBuildDir/sources")
//        destinationDirectory.set(file("$benchmarkBuildDir/classes"))
//        javaCompiler.set(javaCompilerProvider())
          destinationDirectory.convention(
            project.objects.directoryProperty().fileValue(temporaryDir)
          )
        }

      val jarTask: TaskProvider<Jar> =
        project.tasks.register<Jar>(
          "kxbJar${targetName}",
        ) {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Build JAR for JMH compiled files for '${targetName}'"

          isZip64 = true
          archiveClassifier = "JMH"
          manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"

          duplicatesStrategy = DuplicatesStrategy.FAIL

          from(compileTask) {
            exclude("**/*.bin")
          }
          from(prepareResourcesTask)

          destinationDirectory = temporaryDir
          archiveBaseName.set("${project.name}-${targetName}-jmh")
        }

      val runBenchmarkTask: TaskProvider<RunJvmBenchmarkTask> =
        project.tasks.register<RunJvmBenchmarkTask>("benchmark${targetName}") {
          runtimeClasspath.from(jarTask)
        }
    }

    abstract class JS
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin("kotlin$targetName") {
      @get:Input
      abstract val title: String

      @get:Input
      abstract val benchmarksExecutor: Property<JsBenchmarksExecutor>
    }

    abstract class Native
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin("kotlin$targetName") {
      @get:Input
      abstract val title: String
    }

    abstract class Wasm
    @KotlinxBenchmarkPluginInternalApi
    @Inject
    constructor(
      final override val targetName: String
    ) : Kotlin("kotlin$targetName")
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
