package kotlinx.benchmark.gradle.mu.tasks.generate

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.tasks.BaseBenchmarkTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory

@CacheableTask
abstract class BaseGenerateBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseBenchmarkTask() {

  @get:OutputDirectory
  abstract val generatedSources: DirectoryProperty

  @get:OutputDirectory
  abstract val generatedResources: DirectoryProperty

  @get:Classpath
  abstract val inputClasses: ConfigurableFileCollection

  @get:Classpath
  abstract val inputDependencies: ConfigurableFileCollection

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection
}
