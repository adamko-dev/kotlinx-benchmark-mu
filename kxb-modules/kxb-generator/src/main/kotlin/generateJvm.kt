package kotlinx.benchmark.generator

import java.io.File
import java.net.URLClassLoader
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
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
//      urls = urls,
      introspectionClassLoader = introspectionClassLoader,
      logger = logger,
    )
  } finally {
    currentThread.contextClassLoader = originalClassLoader
  }
}

// Based on https://github.com/openjdk/jmh/blob/1.37/jmh-generator-bytecode/src/main/java/org/openjdk/jmh/generators/bytecode/JmhBytecodeGenerator.java
private fun generateJMH(
  inputClasses: Set<File>,
  outputSourceDirectory: File,
  outputResourceDirectory: File,
  introspectionClassLoader: URLClassLoader,
  logger: Logger,
) {
  val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)

  val allFiles = inputClasses.associate { dir ->
    dir.toPath() to FileUtils.getClasses(dir).map { it.toPath() }
  }

  val source = RFGeneratorSource()

  for ((directory, files) in allFiles) {
    logger.log("Analyzing ${files.size} files from $directory")
    for (file in files) {
      val resourceName = file.relativeTo(directory).invariantSeparatorsPathString
      if (resourceName.endsWith(".class")) {
        val className = resourceName
//          .replace('\\', '.')
          .replace('/', '.')
          .removeSuffix(".class")

        val clazz = Class.forName(className, false, introspectionClassLoader)
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
