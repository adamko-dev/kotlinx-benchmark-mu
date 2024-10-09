package kotlinx.benchmark.gradle.mu.tasks

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault(because = "wip")
abstract class RunBenchmarkBaseTask
@KotlinxBenchmarkPluginInternalApi
protected constructor() : KxbBaseTask() {

  @KotlinxBenchmarkPluginInternalApi
  @get:Console // only affects stdout logging
  abstract val ideaActive: Property<Boolean>

  @KotlinxBenchmarkPluginInternalApi
  @get:Input
  @get:Optional
  abstract val enableDemoMode: Property<Boolean>

  init {
    // Trick IntelliJ into thinking this is a test task,
    // so we can log test data via stdout encoded with IJ XML.
    val ideaActiveProvider: String by lazy {
      ideaActive.orNull.toString()
    }
    extensions.extraProperties.set("idea.internal.test", ideaActiveProvider)
  }
}
