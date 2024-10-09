package kotlinx.benchmark.gradle.mu.tooling

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.tools.D8ToolSpec
import kotlinx.benchmark.gradle.mu.config.tools.NodeJsToolSpec
import kotlinx.benchmark.gradle.mu.internal.utils.adding
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance

abstract class JsToolsExtension
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  objects: ObjectFactory
) : ExtensionAware {
  abstract val hostPlatform: Property<Platform>
  abstract val toolsDir: DirectoryProperty

  val d8: D8ToolSpec = extensions.adding("d8", objects.newInstance<D8ToolSpec>())

  val nodeJs: NodeJsToolSpec = extensions.adding("nodeJs", objects.newInstance<NodeJsToolSpec>())
}
