package kotlinx.benchmark.integration

//import org.junit.*
//import org.junit.rules.*
import java.nio.file.Path
import kotlin.io.path.*
import org.junit.jupiter.api.io.TempDir


abstract class GradleTest {
  @TempDir
  internal lateinit var testProjectDir: Path //= TemporaryFolder(File("build/temp").apply { mkdirs() })

  private val rootProjectDir: Path get() = testProjectDir

  fun file(path: String): Path = rootProjectDir.resolve(path)

  fun reports(configuration: String): List<Path> {
    val folder = file("build/reports/benchmarks/$configuration")
    return folder.listDirectoryEntries().flatMap { it?.listDirectoryEntries().orEmpty().toList() }
  }

  @OptIn(ExperimentalPathApi::class)
  fun project(
    name: String,
    print: Boolean = false,
    gradleVersion: GradleTestVersion? = null,
    kotlinVersion: String? = null,
    build: ProjectBuilder.() -> Unit = {}
  ): Runner {
    val builder = ProjectBuilder().apply {
      kotlinVersion?.let { this.kotlinVersion = it }
    }.apply(build)
    templates.resolve(name).copyToRecursively(rootProjectDir, overwrite = true, followLinks = false)
    file("build.gradle").modify(builder::build)
    val settingsFile = file("settings.gradle")
    if (!settingsFile.exists()) {
      file("settings.gradle").writeText("") // empty settings file
    }
    return Runner(rootProjectDir.toFile(), print, gradleVersion)
  }
}

private val templates = Path("src/test/resources/templates")

private fun Path.modify(fn: (String) -> String) {
  writeText(fn(readText()))
}
