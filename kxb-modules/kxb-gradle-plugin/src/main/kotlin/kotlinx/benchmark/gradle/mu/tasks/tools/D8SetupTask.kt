package kotlinx.benchmark.gradle.mu.tasks.tools

import java.net.URI
import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.tools.D8ToolSpec
import kotlinx.benchmark.gradle.mu.internal.utils.Http
import kotlinx.benchmark.gradle.mu.tasks.BaseBenchmarkTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


@KotlinxBenchmarkPluginInternalApi
@CacheableTask
abstract class D8SetupTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseBenchmarkTask() {
  @get:Input
  abstract val version: Property<String>
  /** Release or Debug */
  @get:Input
  abstract val edition: Property<D8ToolSpec.Edition>

  @get:Input
  abstract val platform: Property<String>

  @get:Input
  abstract val downloadBaseUrl: Property<URI>

  @get:OutputDirectory
  abstract val installationDir: DirectoryProperty

  @get:Internal
  abstract val cacheDir: DirectoryProperty

  @TaskAction
  protected fun action() {
    val downloadDir = cacheDir.get().asFile.resolve("download")
    val cacheDir = cacheDir.get().asFile

    // https://storage.googleapis.com/chromium-v8/official/canary/v8-win64-rel-11.4.28.zip
    val zipFilename = "v8-${platform.get()}-${edition.get()}-${version.get()}.zip"
    val src = downloadBaseUrl.get().resolve(zipFilename)

    val dest = downloadDir.resolve(zipFilename)
    val etagFile = cacheDir.resolve("etag.txt")

    val downloadedFile = Http.download(
      src = src.toASCIIString(),
      dest = dest,
      etagFile = etagFile
    )

    fs.sync {
      from(archives.zipTree(downloadedFile))
      into(installationDir)
      includeEmptyDirs = false
    }
  }

  /*
//  private fun getVersion(): String {
//    val requestedVersion = version.get()
//    if (requestedVersion == "latest") {
//      val platform = platform.get()
//      val edition = edition.get().id
//      val latestVersionJsonUrl =
//        "https://storage.googleapis.com/chromium-v8/official/canary/v8-$platform-$edition-latest.json"
//
//      val cacheDir = cacheDir.get().asFile
//      val latestJsonFile = cacheDir.resolve("latest.json")
//
//      ant.get(
//        src = latestVersionJsonUrl,
//        dest = latestJsonFile,
//      )
//
//      val latestJson = JsonSlurper().parse(latestJsonFile) as Map<*, *>
//      val latestVersion = latestJson["version"]?.toString()
//
////      val latestVersion = latestJsonFile.readText()
////        .substringAfter("\"version\": \"")
////        .substringBefore("\"")
//
//      return latestVersion
//        ?: error("Failed to fetch latest D8 version. url:$latestVersionJsonUrl json:$latestJsonFile.")
//    } else {
//      return requestedVersion
//    }
//  }
*/

  internal fun configure(spec: D8ToolSpec) {
    onlyIf("D8ToolSpec is enabled") { spec.enabled.get() }
    edition.convention(spec.edition)
    platform.convention(spec.platform)
    downloadBaseUrl.convention(spec.downloadBaseUrl)
    installationDir.convention(spec.installationDir)
    cacheDir.set(temporaryDir.resolve("cache"))
  }

  companion object
}
