package kotlinx.benchmark.gradle.mu.config.tools

import java.net.URI
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.intellij.lang.annotations.Language

/**
 * Specification for downloading and running [D8](https://v8.dev/docs/d8).
 */
@KotlinxBenchmarkPluginInternalApi
abstract class D8ToolSpec
@KotlinxBenchmarkPluginInternalApi
constructor() : JsToolSpec() {

  abstract val command: Property<String>
  abstract val executable: RegularFileProperty

  abstract val downloadBaseUrl: Property<URI>
  fun downloadBaseUrl(
    @Language("http-url-reference")
    uri: String
  ) {
    downloadBaseUrl.set(URI(uri))
  }

  abstract val installationDir: DirectoryProperty

  abstract val version: Property<String>

  /** Release or Debug */
  abstract val edition: Property<Edition>

  abstract val platform: Property<String>
//  abstract val system: Property<System>
//  abstract val arch: Property<Arch>
//  fun platform(): Provider<String> =
//    system.zip(arch) { system, arch ->
//      getD8Platform(system = system, arch = arch)
//    }

  sealed class Edition(val id: String) {
    object Release : Edition("rel") {
      override fun toString(): String = "D8ToolSpec.Edition.Release"
    }

    object Debug : Edition("dbg") {
      override fun toString(): String = "D8ToolSpec.Edition.Release"
    }

//    class Custom(id: String) : Edition(id) {
//      override fun toString(): String = "D8ToolSpec.Edition.Custom($id)"
//      override fun hashCode(): Int = id.hashCode()
//      override fun equals(other: Any?): Boolean =
//        this === other || (other as? Custom)?.id == id
//    }
  }

//  enum class System {
//    Linux,
//    Mac,
//    Windows,
//  }
//
//  enum class Arch {
//    Arm64,
//    X64,
//    X86,
//  }
}
