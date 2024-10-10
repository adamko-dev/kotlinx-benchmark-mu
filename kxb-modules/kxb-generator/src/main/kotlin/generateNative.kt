package kotlinx.benchmark.generator

import kotlinx.benchmark.generator.internal.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File


@KotlinxBenchmarkGeneratorInternalApi
fun generateNative(
  title: String,
  target: String,
  inputClassesDirs: Set<File>,
  inputDependencies: Set<File>,
  outputSourcesDir: File,
  outputResourcesDir: File,
) {
  outputSourcesDir.deleteRecursively()
  outputResourcesDir.deleteRecursively()

  inputClassesDirs
    .filter { it.exists() && it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }
    .forEach { lib ->
      if (target.isEmpty())
        throw Exception("nativeTarget should be specified for API generator for native targets")

      val storageManager = LockBasedStorageManager("Inspect")
      val module = KlibResolver.Native.createModuleDescriptor(lib, inputDependencies, storageManager)
      val generator = SuiteSourceGenerator(
        title = title,
        module = module,
        output = outputSourcesDir,
        platform = Platform.NativeBuiltIn,
      )
      generator.generate()
    }
}
