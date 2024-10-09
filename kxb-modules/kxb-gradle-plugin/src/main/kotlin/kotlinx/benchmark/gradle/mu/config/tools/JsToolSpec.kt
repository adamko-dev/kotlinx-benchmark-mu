package kotlinx.benchmark.gradle.mu.config.tools

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

/**
 * Base type for determining how to download and run a JS tool.
 */
@KotlinxBenchmarkPluginInternalApi
abstract class JsToolSpec
@KotlinxBenchmarkPluginInternalApi
constructor() : ExtensionAware {
  abstract val enabled: Property<Boolean>
}
