package kotlinx.benchmark.generator

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.generators.core.BenchmarkGenerator
import org.openjdk.jmh.generators.core.FileSystemDestination
import org.openjdk.jmh.generators.reflection.RFGeneratorSource
import org.openjdk.jmh.util.FileUtils

@KotlinxBenchmarkGeneratorInternalApi
fun generateJvm(
  inputClasses: Set<File>,
  inputClasspath: Set<File>,
  outputSourceDirectory: File,
  outputResourceDirectory: File,
  logger: Logger,
) {
  outputSourceDirectory.deleteRecursively()
  outputResourceDirectory.deleteRecursively()

  val urls = (inputClasses + inputClasspath).map { it.toURI().toURL() }.toTypedArray()

  // Include compiled bytecode on classpath, in case we need to
  // resolve the cross-class dependencies
  val benchmarkAnnotation = Benchmark::class.java

  val currentThread = Thread.currentThread()
  val originalClassLoader = currentThread.contextClassLoader

  // TODO: This is some magic I don't understand yet
  // Somehow Benchmark class is loaded into a Launcher/App class loader and not current context class loader
  // Hence, if parent classloader is set to originalClassLoader then Benchmark annotation check doesn't work
  // inside JMH bytecode gen. This hack seem to work, but we need to understand
  val introspectionClassLoader = URLClassLoader(urls, benchmarkAnnotation.classLoader)

  //logger.log("Original_Parent_ParentCL: ${originalClassLoader.parent.parent}")
  //logger.log("Original_ParentCL: ${originalClassLoader.parent}")
  //logger.log("OriginalCL: $originalClassLoader")
  //logger.log("IntrospectCL: $introspectionClassLoader")
  //logger.log("BenchmarkCL: ${benchmarkAnnotation.classLoader}")

  try {
    currentThread.contextClassLoader = introspectionClassLoader
    generateJMH(
      inputClasses = inputClasses,
      outputSourceDirectory = outputSourceDirectory,
      outputResourceDirectory = outputResourceDirectory,
      urls = urls,
      introspectionClassLoader = introspectionClassLoader,
      logger = logger,
    )
  } finally {
    currentThread.contextClassLoader = originalClassLoader
  }
}

private fun generateJMH(
  inputClasses: Set<File>,
  outputSourceDirectory: File,
  outputResourceDirectory: File,
  urls: Array<URL>,
  introspectionClassLoader: URLClassLoader,
  logger: Logger,
) {
  val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)

  val allFiles = HashMap<File, Collection<File>>(urls.size)
  for (directory in inputClasses) {
    val classes = FileUtils.getClasses(directory)
    allFiles[directory] = classes
  }

  val source = RFGeneratorSource()
  for ((directory, files) in allFiles) {
    logger.log("Analyzing ${files.size} files from $directory")
    val directoryPath = directory.absolutePath
    for (file in files) {
      val resourceName = file.absolutePath.substring(directoryPath.length + 1)
      if (resourceName.endsWith(classSuffix)) {
        val className = resourceName.replace('\\', '.').replace('/', '.')
        val clazz = Class.forName(className.removeSuffix(classSuffix), false, introspectionClassLoader)
        source.processClasses(clazz)
      }
    }
  }

  logger.log("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")
  val gen = BenchmarkGenerator()
  gen.generate(source, destination)
  gen.complete(source, destination)

  if (destination.hasErrors()) {
    val errors = destination.errors.joinToString("\n") { err -> "  - $err" }
    throw RuntimeException("Generation of JMH bytecode failed with ${destination.errors.size} errors:\n$errors")
  }
}

private const val classSuffix = ".class"
