package kotlinx.benchmark.gradle.mu.tasks.tools

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.tools.D8ToolSpec
import kotlinx.benchmark.gradle.mu.tasks.KxbBaseTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.PathSensitivity.RELATIVE


@KotlinxBenchmarkPluginInternalApi
abstract class D8ExecTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  /** Arguments for D8. Will be passed first, before [runArguments]. */
  @get:Input
  abstract val d8Arguments: ListProperty<String>

  /** Arguments for the program being executed. Will be passed last, after `--`. */
  @get:Input
  @get:Optional
  abstract val runArguments: ListProperty<String>

  @get:OutputDirectory
  abstract val workingDir: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(NAME_ONLY)
  abstract val executable: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(RELATIVE)
//  @get:Optional
  //@get:NormalizeLineEndings
  abstract val module: RegularFileProperty

  @TaskAction
  protected fun exec() {
    exec.exec {
      this.executable(this@D8ExecTask.executable.asFile.get())
      this.args(buildArgs())
      this.workingDir(this@D8ExecTask.workingDir.asFile.get())
    }
  }

  private fun buildArgs() {
    val d8Args = d8Arguments.orNull.orEmpty()
    val moduleFile = module.get().asFile.absoluteFile.canonicalFile.invariantSeparatorsPath
    val runArguments = runArguments.orNull
    buildList {
      addAll(d8Args)
      add("-module")
      add(moduleFile)
      if (!runArguments.isNullOrEmpty()) {
        add("--")
        addAll(runArguments)
      }
    }
  }

  internal fun configure(spec: D8ToolSpec) {
    onlyIf("D8ToolSpec is enabled") { spec.enabled.get() }
    //command.convention(spec.command)
    executable.convention(spec.executable)
    workingDir.set(temporaryDir)
  }

  companion object {
//    fun create(
//      compilation: KotlinJsIrCompilation,
//      name: String,
//      configuration: D8Exec.() -> Unit = {}
//    ): TaskProvider<D8Exec> {
//      val target = compilation.target
//      val project = target.project
//      val d8 = D8RootPlugin.apply(project.rootProject)
//      return project.registerTask(
//        name
//      ) {
//        it.executable = d8.requireConfigured().executable
//        it.dependsOn(d8.setupTaskProvider)
//        it.dependsOn(compilation.compileTaskProvider)
//        it.configuration()
//      }
//    }
  }
}
