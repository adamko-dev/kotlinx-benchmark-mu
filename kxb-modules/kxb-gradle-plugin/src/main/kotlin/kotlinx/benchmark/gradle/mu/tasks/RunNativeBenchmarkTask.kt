package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class RunNativeBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : RunBenchmarkBaseTask() {

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Nested
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>

  @TaskAction
  fun action() {
  }
}
