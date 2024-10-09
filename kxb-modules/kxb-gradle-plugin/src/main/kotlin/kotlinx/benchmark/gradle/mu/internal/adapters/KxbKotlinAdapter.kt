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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.wasm
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion


/**
 * Automatically register Kotlin source sets as Benchmark targets.
 */
internal abstract class KxbKotlinAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
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
      is KotlinJvmProjectExtension    -> {
        createKotlinJvmBenchmarkTarget(kxbExtension, kotlinExtension.target)
      }

      is KotlinMultiplatformExtension -> {

        val jvmTargets = kotlinExtension.targets.withType<KotlinJvmTarget>()
        jvmTargets.all target@{
          createKotlinJvmBenchmarkTarget(kxbExtension, this@target)
        }

        val nativeTargets = kotlinExtension.targets.withType<KotlinNativeTarget>()
        nativeTargets.all target@{
          createKotlinNativeBenchmarkTarget(kxbExtension, this@target)
        }

        val jsTargets = kotlinExtension.targets.withType<KotlinJsIrTarget>()
          .matching { it.platformType == js }
        jsTargets.all target@{
          createKotlinJsBenchmarkTarget(kxbExtension, kotlinExtension, this@target)
        }

        val wasmJsTargets = kotlinExtension.targets.withType<KotlinJsIrTarget>()
          .matching { it.platformType == wasm }
        wasmJsTargets.all target@{
          registerWasmTarget(this@target)
          kxbExtension.targets.create<BenchmarkTarget.Kotlin.WasmJs>(this@target.targetName) {
          }
        }
      }

      else                            -> {
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
    target: KotlinTarget,
  ) {
//    val compilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    kxbExtension.targets.create<BenchmarkTarget.Kotlin.Native>(
      target.targetName + "Native",
    ) {
      // TODO configure Kotlin Native target
    }
  }

  private fun createKotlinJsBenchmarkTarget(
    kxbExtension: BenchmarkExtension,
    kotlinExtension: KotlinMultiplatformExtension,
    target: KotlinJsIrTarget,
  ) {
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val kotlinJs = kotlinExtension.js()

//      this.runner.convention(
//        when {
//          //target.isD8Configured     -> BenchmarkTarget.Kotlin.JS.JsRunner.D8
//          target.isNodejsConfigured -> BenchmarkTarget.Kotlin.JS.JsRunner.NodeJs
//          else                      -> error("kotlinx-benchmark only supports nodejs() environment for Kotlin/JS")
//        }
//      )

    val benchmarkCompilation: KotlinJsIrCompilation =
      kotlinJs.compilations.create(buildName("benchmark", target.name, BENCHMARK_COMPILATION_SUFFIX)) {

        defaultSourceSet {

          // TODO why set resources to empty?
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
          dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")

          compilerOptions.sourceMap = true
          compilerOptions.moduleKind = JsModuleKind.MODULE_UMD
        }
      }

//    require(benchmarkCompilation.target == kotlinJs) {
//      "${benchmarkCompilation.target} != $kotlinJs"
//    }
    val binary = kotlinJs.binaries.executable(benchmarkCompilation)
      .first { it.mode == KotlinJsBinaryMode.PRODUCTION }

//    println("KxbKotlinAdapter: binary:${binary.name}")

//      benchmarkCompilation.compileTaskProvider.map { task ->
//        task.compilerOptions.moduleName.map { "${it}.js" }
//      }
    val outputFileName = binary.linkTask.flatMap { task ->
      task.compilerOptions.moduleName.map { "${it}.js" }
    }
    val destinationDir = binary.linkSyncTask.flatMap { it.destinationDirectory }
    val executableFile =
//        layout.file(
      destinationDir.zip(outputFileName) { dir, fileName -> dir.resolve(fileName) }
//        )

//    val execTaskFiles = objects.fileCollection().apply {
//      from(executableFile)
//      builtBy(
//        benchmarkCompilation.compileTaskProvider,
//        binary.linkTask,
//        binary.linkSyncTask,
//      )
//    }

    val kxbJsTarget = kxbExtension.targets.create<BenchmarkTarget.Kotlin.JS>(
      target.targetName.ifBlank { "js" },
//      listOf(benchmarkCompilation.compileTaskProvider,
//      binary.linkTask,
//      binary.linkSyncTask),
    ) {

//      this@create.compiledExecutableModule.fileProvider(
//        execTaskFiles.elements.map { it.single().asFile }
//      )

      compiledExecutableModule.from(executableFile)
      compiledExecutableModule.builtBy(
        benchmarkCompilation.compileTaskProvider,
        binary.linkTask,
        binary.linkSyncTask,
      )

      generatorTask.configure {
        inputClasses.from(mainCompilation.output.allOutputs)
//        inputDependencies.from({ mainCompilation.compileDependencyFiles })
//        inputClasses.from(benchmarkCompilation.output.allOutputs)
        inputDependencies.from({ mainCompilation.compileDependencyFiles })
        inputDependencies.from({ benchmarkCompilation.compileDependencyFiles })
      }

      requiredJsFiles.from(
        { benchmarkCompilation.npmProject.require("source-map-support/register.js") }
      )
    }

    benchmarkCompilation.defaultSourceSet {
      kotlin.srcDir(kxbJsTarget.generatorTask)
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

  private fun registerWasmTarget(kotlinTarget: KotlinTarget) {
    val mainCompilation = kotlinTarget.compilations.getByName(MAIN_COMPILATION_NAME)

  }

//  /** Register a [DokkaSourceSetSpec] for each element in [sourceSetDetails] */
//  private fun registerDokkatooSourceSets(
//    dokkatooExtension: DokkatooExtension,
//    sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails>,
//  ) {
//    // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
//    sourceSetDetails.all details@{
//      dokkatooExtension.dokkatooSourceSets.register(details = this@details)
//    }
//  }

//  /** Register a single [DokkaSourceSetSpec] for [details] */
//  private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.register(
//    details: KotlinSourceSetDetails
//  ) {
//    val kssPlatform = details.compilations.map { values: List<KotlinCompilationDetails> ->
//      values.map { it.kotlinPlatform }
//        .distinct()
//        .singleOrNull() ?: KotlinPlatform.Common
//    }
//
//    val kssClasspath = determineClasspath(details)
//
//    register(details.name) dss@{
//      suppress.set(!details.isPublishedSourceSet())
//      sourceRoots.from(details.sourceDirectories)
//      classpath.from(kssClasspath)
//      analysisPlatform.set(kssPlatform)
//      dependentSourceSets.addAllLater(details.dependentSourceSetIds)
//    }
//  }

  private fun determineClasspath(
//    details: KotlinSourceSetDetails
  ): Provider<FileCollection> {
    TODO("...")
//    return details.compilations.map { compilations: List<KotlinCompilationDetails> ->
//      val classpath = objects.fileCollection()
//
//      if (compilations.isNotEmpty()) {
//        compilations.fold(classpath) { acc, compilation ->
//          acc.from(compilation.compilationClasspath)
//        }
//      } else {
//        classpath
//          .from(details.sourceDirectories)
//          .from(details.sourceDirectoriesOfDependents)
//      }
//    }
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
          ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
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

    /** Get the version of the Kotlin Gradle Plugin currently used to compile the project */
    // Must be lazy, else tests fail (because the KGP plugin isn't accessible)
    internal val currentKotlinToolingVersion: KotlinToolingVersion by lazy {
      val kgpVersion = getKotlinPluginVersion(logger)
      KotlinToolingVersion(kgpVersion)
    }
  }
}


///**
// * Store the details of all [KotlinCompilation]s in a configuration cache compatible way.
// *
// * The compilation details may come from a multiplatform project ([KotlinMultiplatformExtension])
// * or a single-platform project ([KotlinSingleTargetExtension]).
// */
//@DokkatooInternalApi
//private data class KotlinCompilationDetails(
//  val target: String,
//  val kotlinPlatform: KotlinPlatform,
//  val allKotlinSourceSetsNames: Set<String>,
//  val publishedCompilation: Boolean,
//  val dependentSourceSetNames: Set<String>,
//  val compilationClasspath: FileCollection,
//  val defaultSourceSetName: String,
//)
//
//
///** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
//private class KotlinCompilationDetailsBuilder(
//  private val objects: ObjectFactory,
//  private val providers: ProviderFactory,
//  private val konanHome: Provider<File>,
//  private val project: Project,
//) {
//
//  fun createCompilationDetails(
//    kotlinProjectExtension: KotlinProjectExtension,
//  ): ListProperty<KotlinCompilationDetails> {
//
//    val details = objects.listProperty<KotlinCompilationDetails>()
//
//    details.addAll(
//      providers.provider {
//        kotlinProjectExtension
//          .allKotlinCompilations()
//          .map { compilation ->
//            createCompilationDetails(compilation = compilation)
//          }
//      })
//
//    return details
//  }
//
//  /** Create a single [KotlinCompilationDetails] for [compilation] */
//  private fun createCompilationDetails(
//    compilation: KotlinCompilation<*>,
//  ): KotlinCompilationDetails {
//    val allKotlinSourceSetsNames =
//      compilation.allKotlinSourceSets.map { it.name } + compilation.defaultSourceSet.name
//
//    val dependentSourceSetNames =
//      compilation.defaultSourceSet.dependsOn.map { it.name }
//
//    val compilationClasspath: FileCollection =
//      collectKotlinCompilationClasspath(compilation = compilation)
//
//    return KotlinCompilationDetails(
//      target = compilation.target.name,
//      kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
//      allKotlinSourceSetsNames = allKotlinSourceSetsNames.toSet(),
//      publishedCompilation = compilation.isPublished(),
//      dependentSourceSetNames = dependentSourceSetNames.toSet(),
//      compilationClasspath = compilationClasspath,
//      defaultSourceSetName = compilation.defaultSourceSet.name
//    )
//  }
//
private fun KotlinProjectExtension.allKotlinCompilations(): Collection<KotlinCompilation<*>> =
  when (this) {
    is KotlinMultiplatformExtension   -> targets.flatMap { it.compilations }
    is KotlinSingleTargetExtension<*> -> target.compilations
    else                              -> emptyList() // shouldn't happen?
  }
//
//  /**
//   * Get the [Configuration][org.gradle.api.artifacts.Configuration] names of all configurations
//   * used to build this [KotlinCompilation] and
//   * [its source sets][KotlinCompilation.kotlinSourceSets].
//   */
//  private fun collectKotlinCompilationClasspath(
//    compilation: KotlinCompilation<*>,
//  ): FileCollection {
//    val compilationClasspath = objects.fileCollection()
//
//    compilationClasspath.from(
//      kotlinCompileDependencyFiles(compilation)
//    )
//
//    compilationClasspath.from(
//      kotlinNativeDependencies(compilation)
//    )
//
//    return compilationClasspath
//  }
//
//  private fun kotlinCompileDependencyFiles(
//    compilation: KotlinCompilation<*>,
//  ): Provider<FileCollection> {
//    return project.configurations
//      .named(compilation.compileDependencyConfigurationName)
//      .map {
//        it.incoming
//          .artifactView {
//            // Android publishes many variants, which can cause Gradle to get confused,
//            // so specify that we need a JAR and resolve leniently
//            if (compilation.target.platformType == KotlinPlatformType.androidJvm) {
//              attributes { artifactType("jar") }
//              lenient(true)
//            }
//            // 'Regular' Kotlin compilations have non-JAR files (e.g. Kotlin/Native klibs),
//            // so don't add attributes for non-Android projects.
//          }
//          .artifacts
//          .artifactFiles
//      }
//  }
//
//  private fun kotlinNativeDependencies(
//    compilation: KotlinCompilation<*>,
//  ): Provider<FileCollection> {
//
//    // apply workaround for Kotlin/Native, which will be fixed in Kotlin 2.0
//    // (see KT-61559: K/N dependencies will be part of `compilation.compileDependencyFiles`)
//    return if (
//      currentKotlinToolingVersion < KotlinToolingVersion("2.0.0")
//      &&
//      compilation is AbstractKotlinNativeCompilation
//    ) {
//      konanHome.map { konanHome ->
//        val konanDistribution = KonanDistribution(konanHome)
//
//        val dependencies = objects.fileCollection()
//
//        dependencies.from(konanDistribution.stdlib)
//
//        // Konan library files for a specific target
//        dependencies.from(
//          konanDistribution.platformLibsDir
//            .resolve(compilation.target.name)
//            .listFiles()
//            .orEmpty()
//            .filter { it.isDirectory || it.extension == "klib" }
//        )
//      }
//    } else {
//      return providers.provider { objects.fileCollection() }
//    }
//  }
//
//  companion object {
//
//    /**
//     * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
//     * when creating a Dokka publication.
//     *
//     * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
//     * This can be overridden manually, though.
//     *
//     * @see DokkaSourceSetSpec.suppress
//     */
//    private fun KotlinCompilation<*>.isPublished(): Boolean {
//      return when (this) {
//        is KotlinMetadataCompilation<*> -> true
//
//        is KotlinJvmAndroidCompilation  -> {
//          // Use string-based comparison, not the actual classes, because AGP has deprecated and
//          // moved the Library/Application classes to a different package.
//          // Using strings is more widely compatible.
//          val variantName = androidVariant::class.jvmName
//          "LibraryVariant" in variantName || "ApplicationVariant" in variantName
//        }
//
//        else                            ->
//          name == MAIN_COMPILATION_NAME
//      }
//    }
//  }
//}


///**
// * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
// *
// * @param[named] Should be [KotlinSourceSet.getName]
// */
//@DokkatooInternalApi
//private abstract class KotlinSourceSetDetails @Inject constructor(
//  private val named: String,
//) : Named {
//
//  /** Direct source sets that this source set depends on */
//  abstract val dependentSourceSetIds: SetProperty<DokkaSourceSetIdSpec>
//  abstract val sourceDirectories: ConfigurableFileCollection
//  /** _All_ source directories from any (recursively) dependant source set */
//  abstract val sourceDirectoriesOfDependents: ConfigurableFileCollection
//  /** The specific compilations used to build this source set */
//  abstract val compilations: ListProperty<KotlinCompilationDetails>
//
//  /** Estimate if this Kotlin source set contains 'published' sources */
//  fun isPublishedSourceSet(): Provider<Boolean> =
//    compilations.map { values ->
//      values.any { it.publishedCompilation }
//    }
//
//  override fun getName(): String = named
//}


///** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
//private class KotlinSourceSetDetailsBuilder(
//  private val sourceSetScopeDefault: Provider<String>,
//  private val objects: ObjectFactory,
//  private val providers: ProviderFactory,
//  /** Used for logging */
//  private val projectPath: String,
//) {
//
//  private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)
//
//  fun createSourceSetDetails(
//    kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
//    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
//  ): NamedDomainObjectContainer<KotlinSourceSetDetails> {
//
//    val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)
//
//    kotlinSourceSets.configureEach kss@{
//      sourceSetDetails.register(
//        kotlinSourceSet = this,
//        allKotlinCompilationDetails = allKotlinCompilationDetails,
//      )
//    }
//
//    return sourceSetDetails
//  }
//
//  private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
//    kotlinSourceSet: KotlinSourceSet,
//    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
//  ) {
//
//    // TODO: Needs to respect filters.
//    //  We probably need to change from "sourceRoots" to support "sourceFiles"
//    //  https://github.com/Kotlin/dokka/issues/1215
//    val extantSourceDirectories = providers.provider {
//      kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
//    }
//
//    val compilations = allKotlinCompilationDetails.map { allCompilations ->
//      allCompilations.filter { compilation ->
//        kotlinSourceSet.name in compilation.allKotlinSourceSetsNames
//      }
//    }
//
//    // determine the source sets IDs of _other_ source sets that _this_ source depends on.
//    val dependentSourceSets = providers.provider { kotlinSourceSet.dependsOn }
//    val dependentSourceSetIds =
//      providers.zip(
//        dependentSourceSets,
//        sourceSetScopeDefault,
//      ) { sourceSets, sourceSetScope ->
//        logger.info("[$projectPath] source set ${kotlinSourceSet.name} has ${sourceSets.size} dependents ${sourceSets.joinToString { it.name }}")
//        sourceSets.map { dependedKss ->
//          objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
//        }
//      }
//
//    val sourceDirectoriesOfDependents = providers.provider {
//      kotlinSourceSet
//        .allDependentSourceSets()
//        .fold(objects.fileCollection()) { acc, sourceSet ->
//          acc.from(sourceSet.kotlin.sourceDirectories)
//        }
//    }
//
//    register(kotlinSourceSet.name) {
//      this.dependentSourceSetIds.addAll(dependentSourceSetIds)
//      this.sourceDirectories.from(extantSourceDirectories)
//      this.sourceDirectoriesOfDependents.from(sourceDirectoriesOfDependents)
//      this.compilations.addAll(compilations)
//    }
//  }
//
//
//  /**
//   * Return a list containing _all_ source sets that this source set depends on,
//   * searching recursively.
//   *
//   * @see KotlinSourceSet.dependsOn
//   */
//  private tailrec fun KotlinSourceSet.allDependentSourceSets(
//    queue: Set<KotlinSourceSet> = dependsOn.toSet(),
//    allDependents: List<KotlinSourceSet> = emptyList(),
//  ): List<KotlinSourceSet> {
//    val next = queue.firstOrNull() ?: return allDependents
//    return next.allDependentSourceSets(
//      queue = (queue - next) union next.dependsOn,
//      allDependents = allDependents + next,
//    )
//  }
//}

private fun KotlinDependencyHandler.npm(
  dependency: String,
  version: Provider<String>,
): Provider<Dependency> {
  return version.map { npm(dependency, it) }
}
