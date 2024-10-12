package kotlinx.benchmark.generator

import kotlinx.benchmark.generator.internal.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File


@KotlinxBenchmarkGeneratorInternalApi
fun generateJs(
  title: String,
  inputClasses: Set<File>,
  inputDependencies: Set<File>,
  outputSourcesDir: File,
  outputResourcesDir: File,
  useBenchmarkJs: Boolean,
) {
  outputSourcesDir.deleteRecursively()
  outputResourcesDir.deleteRecursively()

  inputClasses.forEach { lib: File ->
    generateSources(
      title = title,
      lib = lib,
      inputDependencies = inputDependencies,
      outputSourcesDir = outputSourcesDir,
      useBenchmarkJs = useBenchmarkJs,
    )
  }
}

@KotlinxBenchmarkGeneratorInternalApi
private fun generateSources(
  title: String,
  lib: File,
  inputDependencies: Set<File>,
  outputSourcesDir: File,
  useBenchmarkJs: Boolean,
) {
  val modules = loadIr(
    lib = lib,
    inputDependencies = inputDependencies,
    storageManager = LockBasedStorageManager("Inspect"),
  )
  modules.forEach { module ->
    val generator = SuiteSourceGenerator(
      title = title,
      module = module,
      output = outputSourcesDir,
      platform = if (useBenchmarkJs) Platform.JsBenchmarkJs else Platform.JsBuiltIn
    )
    generator.generate()
  }
}

private fun loadIr(
  lib: File,
  inputDependencies: Set<File>,
  storageManager: StorageManager,
): List<ModuleDescriptor> {
  // skip processing of empty dirs (fails if not to do it)
  if (lib.listFiles() == null) return emptyList()
  val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
  val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
  return listOf(module)
}
