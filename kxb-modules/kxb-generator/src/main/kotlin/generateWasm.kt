package kotlinx.benchmark.generator

import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import kotlinx.benchmark.generator.internal.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File


@KotlinxBenchmarkGeneratorInternalApi
fun generateWasm(
  title: String,
  inputClasses: Set<File>,
  inputDependencies: Set<File>,
  outputSourcesDir: File,
  outputResourcesDir: File,
) {
  outputSourcesDir.deleteRecursively()
  outputResourcesDir.deleteRecursively()

  inputClasses.forEach { lib: File ->
    generateSources(
      title,
      lib,
      inputDependencies = inputDependencies,
      outputSourcesDir,
    )
  }
}

private fun generateSources(
  title: String,
  lib: File,
  inputDependencies: Set<File>,
  outputSourcesDir: File,
) {
  val modules = loadIr(
    lib,
    inputDependencies = inputDependencies,
    LockBasedStorageManager("Inspect"),
  )
  modules.forEach { module ->
    val generator = SuiteSourceGenerator(
      title,
      module,
      outputSourcesDir,
      Platform.WasmBuiltIn
    )
    generator.generate()
  }
}

private fun loadIr(
  lib: File,
  inputDependencies: Set<File>,
  storageManager: StorageManager,
): List<ModuleDescriptor> {
  //skip processing of empty dirs (fail if not to do it)
  if (lib.listFiles() == null) return emptyList()
  val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
  val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
  return listOf(module)
}
