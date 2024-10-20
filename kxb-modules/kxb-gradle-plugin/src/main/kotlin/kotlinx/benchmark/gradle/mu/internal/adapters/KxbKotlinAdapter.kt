package kotlinx.benchmark.gradle.mu.internal.adapters

import java.io.File
import javax.inject.Inject
import kotlinx.benchmark.gradle.BenchmarksPlugin
import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.BENCHMARK_COMPILATION_SUFFIX
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.BenchmarkExtension
import kotlinx.benchmark.gradle.mu.BenchmarkPlugin
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import kotlinx.benchmark.gradle.mu.internal.utils.PluginId
import kotlinx.benchmark.gradle.mu.internal.utils.buildName
import kotlinx.benchmark.gradle.mu.internal.utils.warn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.wasm
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget


/**
 * Automatically register Kotlin source sets as Benchmark targets.
 */
internal abstract class KxbKotlinAdapter @Inject constructor(
  private val layout: ProjectLayout,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("Applying $clsName to ${project.path}")

    project.plugins.withType<BenchmarkPlugin>().configureEach {
      val kxbExtension = project.extensions.getByType<BenchmarkExtension>()

      project.pluginManager.apply {
        withPlugin(PluginId.KotlinAndroid) { handleKotlin(project, kxbExtension) }
        withPlugin(PluginId.KotlinJs) { handleKotlin(project, kxbExtension) }
        withPlugin(PluginId.KotlinJvm) { handleKotlin(project, kxbExtension) }
        withPlugin(PluginId.KotlinMultiplatform) { handleKotlin(project, kxbExtension) }
      }
    }
  }

  private fun handleKotlin(
    project: Project,
    kxbExtension: BenchmarkExtension,
  ) {
    val kotlinExtension = project.extensions.findKotlinExtension()
    if (kotlinExtension == null) {
      handleMissingKotlinExtension(project)
      return
    }
    logger.info("Configuring $clsName in Gradle Kotlin Project ${project.path}")

    when (kotlinExtension) {
      is KotlinJvmProjectExtension -> {
        createKotlinJvmBenchmarkTarget(kxbExtension, kotlinExtension.target)
      }

      is KotlinMultiplatformExtension -> {

        kotlinExtension.targets.withType<KotlinJvmTarget>().all target@{
          createKotlinJvmBenchmarkTarget(kxbExtension, this@target)
        }

        kotlinExtension.targets.withType<KotlinNativeTarget>().all target@{
          createKotlinNativeBenchmarkTarget(kxbExtension, this@target)
        }

        kotlinExtension.targets.withType<KotlinJsIrTarget>()
          .matching { it.platformType == js }
          .all target@{
            createKotlinJsBenchmarkTarget(kxbExtension, this@target, project = project)
          }

        kotlinExtension.targets.withType<KotlinJsIrTarget>()
          .matching { it.platformType == wasm }
          .all target@{
            registerWasmTarget(kxbExtension, this@target, project = project)
          }
      }

      else -> {
        error("Unexpected 'kotlin' extension $kotlinExtension")
      }
    }
  }

  private fun createKotlinJvmBenchmarkTarget(
    kxbExtension: BenchmarkExtension,
    target: KotlinTarget,
  ) {
    val compilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    kxbExtension.targets.create<BenchmarkTarget.Kotlin.JVM>(
      target.targetName.ifBlank { "jvm" },
    ) {

      targetCompilationDependencies.from({ compilation.compileDependencyFiles })
      targetRuntimeDependencies.from({ compilation.runtimeDependencyFiles })
      compiledTarget.from({ compilation.output.allOutputs })

      generateBenchmarkTask.configure {
        dependsOn(compilation.compileTaskProvider)
        inputCompileClasspath.from({ compilation.compileDependencyFiles })
        inputClasses.from(compilation.output.allOutputs)
      }
    }
  }

  private fun createKotlinNativeBenchmarkTarget(
    kxbExtension: BenchmarkExtension,
    target: KotlinNativeTarget,
  ) {
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val benchmarkCompilation = target.compilations.create(
      buildName("benchmark", target.name, BENCHMARK_COMPILATION_SUFFIX)
    ) {
      defaultSourceSet {
        // benchmarks compilations don't need resources, so remove resource srcDirs to avoid unnecessary dirs
        resources.setSrcDirs(emptyList<File>())

        dependencies {
          implementation(mainCompilation.output.allOutputs)
          //{ mainCompilation.compileDependencyFiles }
        }
      }
    }

    val benchmarkCompilationCompileDependencyConfiguration =
      target.project.configurations.named(benchmarkCompilation.implementationConfigurationName)
    val compilationCompileDependencyConfiguration =
      target.project.configurations.named(mainCompilation.compileDependencyConfigurationName)
    benchmarkCompilationCompileDependencyConfiguration.configure {
      extendsFrom(compilationCompileDependencyConfiguration.get())
    }

    benchmarkCompilation.compileTaskProvider.configure {
      compilerOptions.freeCompilerArgs.addAll(
        mainCompilation.compileTaskProvider.flatMap { it.compilerOptions.freeCompilerArgs }
      )
    }

    val nativeBenchmarkTarget = kxbExtension.targets.create<BenchmarkTarget.Kotlin.Native>(
      target.targetName
    ) {
      this.title.convention("${target.project.displayName} ${target.targetName}")
      this.forkMode.convention(BenchmarkTarget.Kotlin.Native.ForkMode.PerBenchmark)
    }

    benchmarkCompilation.defaultSourceSet {
      kotlin.srcDir(nativeBenchmarkTarget.generatorTask)
    }

    val binary = target.binaries.createExecutable(
      namePrefix = benchmarkCompilation.name,
      buildType = NativeBuildType.RELEASE,
    ) { exe ->
      exe.compilation = benchmarkCompilation
      exe.entryPoint("kotlinx.benchmark.generated.main")
      exe.linkTaskProvider.configure {
        dependsOn(nativeBenchmarkTarget.generatorTask)
      }
    }

    nativeBenchmarkTarget.executable.set(
      binary.linkTaskProvider.map {
        layout.file(it.outputFile).get()
      }
    )

    nativeBenchmarkTarget.generatorTask.configure {
      this.inputClasses.from(mainCompilation.output.allOutputs)
      this.inputDependencies.from({ mainCompilation.compileDependencyFiles })
    }
  }

  private fun createKotlinJsBenchmarkTarget(
    kxbExtension: BenchmarkExtension,
    target: KotlinJsIrTarget,
    project: Project,
  ) {
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    mainCompilation.defaultSourceSet {
      dependencies {
        implementation(npm("source-map-support", version = kxbExtension.versions.jsSourceMapSupport))
      }
    }

//    val kotlinJs = kotlinExtension.js()

//      this.runner.convention(
//        when {
//          //target.isD8Configured     -> BenchmarkTarget.Kotlin.JS.JsRunner.D8
//          target.isNodejsConfigured -> BenchmarkTarget.Kotlin.JS.JsRunner.NodeJs
//          else                      -> error("kotlinx-benchmark only supports nodejs() environment for Kotlin/JS")
//        }
//      )

    val benchmarkCompilation: KotlinJsIrCompilation =
      target.compilations.create(buildName(target.name, mainCompilation.name, "benchmark")) {

        this.compileTaskProvider.configure {
          dependsOn(mainCompilation.compileTaskProvider)
          dependsOn(mainCompilation.compileAllTaskName)
          dependsOn(mainCompilation.compileKotlinTaskName)
        }

        defaultSourceSet {
          // benchmarks compilations don't need resources, so remove resource srcDirs to avoid unnecessary dirs
          resources.setSrcDirs(emptyList<File>())

          dependencies {
            implementation(mainCompilation.output.allOutputs)
            implementation(npm("benchmark", version = kxbExtension.versions.benchmarkJs))
            implementation(npm("source-map-support", version = kxbExtension.versions.jsSourceMapSupport))
          }
        }

        project.configurations.named(implementationConfigurationName) {
          extendsFrom(
            project.configurations.getByName(mainCompilation.compileDependencyConfigurationName)
          )
        }

        compileTaskProvider.configure {
          group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
          description = "Compile JS benchmark source files for '${target.name}'"

          //TODO: fix destination dir after KT-29711 is fixed
          //println("JS: ${kotlinOptions.outputFile}")
          //destinationDir = file("$benchmarkBuildDir/classes")

          compilerOptions.sourceMap = true
          compilerOptions.moduleKind = JsModuleKind.MODULE_UMD
        }
      }

    val benchmarkBinary = target.binaries.executable(benchmarkCompilation)
      .firstOrNull { it.mode == KotlinJsBinaryMode.PRODUCTION }
      ?: error("Failed to get Kotlin/JS executable production binary for target ${target.name}")

    val moduleJs = providers.zip(
      benchmarkBinary.linkTask,
      benchmarkBinary.linkSyncTask,
    ) { link, linkSync ->
      val moduleJsFileName = link.compilerOptions.moduleName.get() + ".js"
      linkSync.destinationDirectory.get().resolve(moduleJsFileName)
    }

    val jsBenchmarkTarget = kxbExtension.targets.create<BenchmarkTarget.Kotlin.JS>(
      target.targetName.ifBlank { "js" },
    ) {

      compiledExecutableModule.convention(
        layout.file(moduleJs)
      )

      generatorTask.configure {
        inputClasses.from(mainCompilation.output.allOutputs)
        inputDependencies.from({ mainCompilation.compileDependencyFiles })
        inputDependencies.from({ benchmarkCompilation.compileDependencyFiles })
      }

      requiredJsFiles.from(
        kxbExtension.kotlinJsNodeModulesDir.map { it.file("source-map-support/register.js") },
      )
    }

    benchmarkCompilation.defaultSourceSet {
      kotlin.srcDir(jsBenchmarkTarget.generatorTask)
    }
  }

  private fun registerWasmTarget(
    kxbExtension: BenchmarkExtension,
    target: KotlinJsIrTarget,
    project: Project,
  ) {
    val mainCompilation: KotlinJsIrCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val benchmarkCompilation =
      target.compilations.create(buildName(target.name, mainCompilation.name, "benchmark")) {

        defaultSourceSet {
          // benchmarks compilations don't need resources, so remove resource srcDirs to avoid unnecessary dirs
          resources.setSrcDirs(emptyList<File>())

          dependencies {
            implementation(mainCompilation.output.allOutputs)
          }

          project.configurations.named(implementationConfigurationName) {
            extendsFrom(
              project.configurations.getByName(mainCompilation.compileDependencyConfigurationName)
            )
          }
        }
      }

    val benchmarkBinary = target.binaries.executable(benchmarkCompilation)
      .firstOrNull { it.mode == KotlinJsBinaryMode.PRODUCTION }
      ?: error("Failed to get Kotlin/WasmJS executable production binary for target ${target.name}")

    val moduleJs = providers.zip(
      benchmarkBinary.linkTask,
      benchmarkBinary.linkSyncTask,
    ) { link, linkSync ->
      val moduleJsFileName = link.compilerOptions.moduleName.get() + ".mjs"
      linkSync.destinationDirectory.get().resolve(moduleJsFileName)
    }

    val wasmJsBenchmarkTarget = kxbExtension.targets.create<BenchmarkTarget.Kotlin.WasmJs>(
      target.targetName.ifBlank { "wasm" },
    ) {
      compiledExecutableModule.convention(
        layout.file(moduleJs)
      )

      generatorTask.configure {
        inputClasses.from(mainCompilation.output.allOutputs)
        inputDependencies.from({ mainCompilation.compileDependencyFiles })
        inputDependencies.from({ benchmarkCompilation.compileDependencyFiles })
      }
    }

    benchmarkCompilation.defaultSourceSet {
      kotlin.srcDir(wasmJsBenchmarkTarget.generatorTask)
    }
  }


  private fun handleMissingKotlinExtension(project: Project) {
    if (project.extensions.findByName("kotlin") != null) {
      // uh oh - the Kotlin extension is present but findKotlinExtension() failed.
      // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
      logger.warn {
        val allPlugins =
          project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
        val allExtensions =
          project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

        /* language=TEXT */
        """
        |$clsName failed to get KotlinProjectExtension in ${project.path}
        |  Applied plugins: $allPlugins
        |  Available extensions: $allExtensions
        """.trimMargin()
      }
    }
    logger.info("Skipping applying $clsName in ${project.path} - could not find KotlinProjectExtension")
  }


  @KotlinxBenchmarkPluginInternalApi
  companion object {
    private val clsName: String = KxbKotlinAdapter::class.simpleName!!

    private val logger = Logging.getLogger(KxbKotlinAdapter::class.java)

    /** Try and get [KotlinProjectExtension], or `null` if it's not present */
    private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
      try {
        findByType()
        // fallback to trying to get the JVM extension
        // (not sure why I did this... maybe to be compatible with really old versions?)
          ?: findByType<KotlinJvmProjectExtension>()
      } catch (e: Throwable) {
        when (e) {
          is TypeNotPresentException,
          is ClassNotFoundException,
          is NoClassDefFoundError -> {
            logger.info("$clsName failed to find KotlinExtension ${e::class} ${e.message}")
            null
          }

          else                    -> throw e
        }
      }

//    /** Get the version of the Kotlin Gradle Plugin currently used to compile the project */
//    // Must be lazy, else tests fail (because the KGP plugin isn't accessible)
//    internal val currentKotlinToolingVersion: KotlinToolingVersion by lazy {
//      val kgpVersion = getKotlinPluginVersion(logger)
//      KotlinToolingVersion(kgpVersion)
//    }
  }
}

/**
 * Create an executable and return it.
 */
private fun KotlinNativeBinaryContainer.createExecutable(
  namePrefix: String,
  buildType: NativeBuildType,
  configure: (executable: Executable) -> Unit,
): Executable {
  executable(
    namePrefix = namePrefix,
    buildTypes = listOf(buildType),
    configure = { configure(this) },
  )
  return getExecutable(
    namePrefix = namePrefix,
    buildType = buildType,
  )
}

/**
 * Create a npm dependency, with a lazily evaluated version.
 */
private fun KotlinDependencyHandler.npm(
  dependency: String,
  version: Provider<String>,
): Provider<Dependency> {
  return version.map { npm(dependency, it) }
}
