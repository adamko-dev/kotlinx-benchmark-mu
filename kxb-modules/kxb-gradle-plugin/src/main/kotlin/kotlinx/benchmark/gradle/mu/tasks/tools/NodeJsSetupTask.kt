package kotlinx.benchmark.gradle.mu.tasks.tools

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.Http
import kotlinx.benchmark.gradle.mu.internal.utils.dropDirectories
import kotlinx.benchmark.gradle.mu.tasks.BaseBenchmarkTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


@KotlinxBenchmarkPluginInternalApi
@CacheableTask
abstract class NodeJsSetupTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseBenchmarkTask() {
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

  @get:Internal
  abstract val cacheDir: DirectoryProperty

  @TaskAction
  protected fun action() {
    val downloadDir = cacheDir.get().asFile.resolve("download")
    val cacheDir = cacheDir.get().asFile

    // https://nodejs.org/dist/v20.18.0/node-v20.18.0-darwin-arm64.tar.gz
    // https://nodejs.org/dist/v20.18.0/node-v20.18.0-win-x86.zip
//    val version = version.get()
//    val system = system.get()
//    val arch = arch.get()
//    val archiveExtension = archiveExtension.get()
//    val nodeJsDistFilename =
//      URLEncoder.encode("v$version/node-v$version-$system-$arch.$archiveExtension", Charsets.UTF_8.name())

    val src = distDownloadUrl.get()
    val dest = downloadDir.resolve(src.substringAfterLast("/"))
    val etagFile = cacheDir.resolve("etag.txt")

    logger.lifecycle("[$path] downloading NodeJS from $src into $dest")

    val downloadedFile = Http.download(
      src = src,
      dest = dest,
      etagFile = etagFile
    )

    fs.sync {
      from(
        when {
          dest.name.endsWith(".zip")    -> archives.zipTree(downloadedFile)
          dest.name.endsWith(".tar.gz") -> archives.tarTree(downloadedFile)
          else                          -> error("Unsupported archive extension: $downloadedFile")
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
      includeEmptyDirs = false
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
