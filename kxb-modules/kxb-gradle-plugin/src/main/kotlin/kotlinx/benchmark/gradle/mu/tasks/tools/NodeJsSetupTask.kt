package kotlinx.benchmark.gradle.mu.tasks.tools

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.dropDirectories
import kotlinx.benchmark.gradle.mu.internal.utils.get
import kotlinx.benchmark.gradle.mu.tasks.KxbBaseTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


@KotlinxBenchmarkPluginInternalApi
@CacheableTask
abstract class NodeJsSetupTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {
//  @get:Input
//  abstract val version: Property<String>

//  @get:Input
//  abstract val system: Property<String>
//
//  @get:Input
//  abstract val arch: Property<String>
//
//  @get:Input
//  abstract val archiveExtension: Property<String>

  @get:Input
  abstract val distDownloadUrl: Property<String>

  @get:OutputDirectory
  abstract val installationDir: DirectoryProperty

  @get:LocalState
  abstract val cacheDir: DirectoryProperty

  @TaskAction
  protected fun action() {
    fs.delete { delete(cacheDir) }

    // https://nodejs.org/dist/v20.18.0/node-v20.18.0-darwin-arm64.tar.gz
    // https://nodejs.org/dist/v20.18.0/node-v20.18.0-win-x86.zip
//    val version = version.get()
//    val system = system.get()
//    val arch = arch.get()
//    val archiveExtension = archiveExtension.get()
//    val nodeJsDistFilename =
//      URLEncoder.encode("v$version/node-v$version-$system-$arch.$archiveExtension", Charsets.UTF_8.name())

    val src = distDownloadUrl.get()
    val dest = cacheDir.get().asFile.resolve(src.substringAfterLast("/"))
    dest.parentFile.mkdirs()

    logger.lifecycle("[$path] downloading NodeJS from $src into $dest")

    ant.get(
      src = src,
      dest = dest,
    )

    fs.sync {
      from(
        when {
          dest.name.endsWith(".zip")    -> archives.zipTree(dest)
          dest.name.endsWith(".tar.gz") -> archives.tarTree(dest)
          else                          -> error("Unsupported archive extension: $dest")
        }
      )
      into(installationDir)
      eachFile {
        relativePath = relativePath.dropDirectories(1)

        if (file.parentFile.name == "bin") {
          permissions {
            group { execute = true }
            user { execute = true }
            //other { execute = true }
          }
        }
      }
    }

    @Suppress("UnstableApiUsage")
    run {
      val r =
        providers.exec {
          executable(installationDir.get().file("bin/node").asFile)
          args("--version")
//      standardOutput = System.out
//      errorOutput = System.err
        }

      logger.lifecycle("[$path] node version ${r.standardOutput.asText.get()}")
    }
  }

  companion object
}
