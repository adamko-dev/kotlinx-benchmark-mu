package kotlinx.benchmark.gradle.mu.internal.utils

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.jvm.optionals.getOrNull
import org.gradle.api.logging.Logging
import org.intellij.lang.annotations.Language

internal object Http {

  private val logger = Logging.getLogger(Http::class.java)

  fun download(
    @Language("http-url-reference")
    src: String,
    dest: File,
    etagFile: File,
  ): File {
    dest.parentFile.mkdirs()

    val etag = etagFile.takeIf { it.exists() }?.readText()
    logger.info("[httpGet] loaded ETag $etag from $etagFile for $src")

    val request = HttpRequest {
      GET()
      uri(URI(src))
      if (dest.exists() && !etag.isNullOrBlank()) {
        header("If-None-Match", etag)
      }
    }

    val client = HttpClient.newHttpClient()
    val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

    if (response.statusCode() == 304 /* Not modified */) {
      // The ETag was up to date, so don't re-download.
      logger.info("[httpGet] ETag $etag up to date for $src")
      return dest
    } else {
      logger.info("[httpGet] ETag $etag was outdated $src")
      response.body().buffered().use { input ->
        Files.copy(input, dest.toPath(), REPLACE_EXISTING)
      }

      val newEtag = response.headers().firstValue("ETag").getOrNull()

      if (newEtag != null && newEtag != etag) {
        logger.info("[httpGet] updating ETag from $etag to $newEtag for $src")
        // Update the stored ETag.
        etagFile.apply {
          parentFile.mkdirs()
          writeText(newEtag)
        }
      }

      return dest
    }
  }

  private fun HttpRequest(build: HttpRequest.Builder.() -> Unit): HttpRequest =
    HttpRequest.newBuilder().apply(build).build()
}
