package kotlinx.benchmark.gradle.mu.config.tools

import java.net.URI
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.intellij.lang.annotations.Language

@KotlinxBenchmarkPluginInternalApi
abstract class NodeJsToolSpec
@KotlinxBenchmarkPluginInternalApi
constructor() : JsToolSpec() {

  abstract val command: Property<String>
  abstract val version: Property<String>

  abstract val distDownloadUrl: Property<String>

  abstract val installationDir: DirectoryProperty

//  abstract val downloadBaseUrl: Property<URI>
//  fun downloadBaseUrl(
//    @Language("http-url-reference")
//    uri: String
//  ) {
//    downloadBaseUrl.set(URI(uri))
//  }
}
