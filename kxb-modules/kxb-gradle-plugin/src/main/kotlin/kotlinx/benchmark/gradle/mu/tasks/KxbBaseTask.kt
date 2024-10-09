package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor


@DisableCachingByDefault(because = "wip")
abstract class KxbBaseTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : DefaultTask() {
  @get:Inject
  protected open val workers: WorkerExecutor get() = error("injected")
  @get:Inject
  protected open val fs: FileSystemOperations get() = error("injected")
  @get:Inject
  protected open val archives: ArchiveOperations get() = error("injected")
  @get:Inject
  protected open val providers: ProviderFactory get() = error("injected")
  @get:Inject
  protected open val objects: ObjectFactory get() = error("injected")
  @get:Inject
  protected open val layout: ProjectLayout get() = error("injected")
  @get:Inject
  protected open val exec: ExecOperations get() = error("injected")

  init {
    group = TASK_GROUP
  }

  companion object {
    const val TASK_GROUP: String = "benchmark"
  }
}
