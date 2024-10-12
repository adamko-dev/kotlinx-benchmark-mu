package kotlinx.benchmark.generator.internal

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

@KotlinxBenchmarkGeneratorInternalApi
enum class Platform(
  val executorClass: String,
  val suiteDescriptorClass: String,
  val benchmarkDescriptorClass: String,
  val benchmarkDescriptorWithBlackholeParameterClass: String
) {
  JsBuiltIn(
    executorClass = "kotlinx.benchmark.js.JsBuiltInExecutor",
    suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
    benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
    benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
  ),
  JsBenchmarkJs(
    executorClass = "kotlinx.benchmark.js.JsBenchmarkExecutor",
    suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
    benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
    benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
  ),
  NativeBuiltIn(
    executorClass = "kotlinx.benchmark.native.NativeExecutor",
    suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
    benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
    benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
  ),
  WasmBuiltIn(
    executorClass = "kotlinx.benchmark.wasm.WasmBuiltInExecutor",
    suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
    benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
    benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
  )
}


@KotlinxBenchmarkGeneratorInternalApi
class SuiteSourceGenerator(
  val title: String,
  val module: ModuleDescriptor,
  val output: File,
  val platform: Platform,
) {

  @KotlinxBenchmarkGeneratorInternalApi
  @Suppress("ConstPropertyName")
  companion object {
    const val setupFunctionName = "setUp"
    const val teardownFunctionName = "tearDown"
    const val parametersFunctionName = "parametrize"

    //const val externalConfigurationFQN = "kotlinx.benchmark.ExternalConfiguration"
    const val benchmarkAnnotationFQN = "kotlinx.benchmark.Benchmark"
    const val setupAnnotationFQN = "kotlinx.benchmark.Setup"
    const val teardownAnnotationFQN = "kotlinx.benchmark.TearDown"
    const val stateAnnotationFQN = "kotlinx.benchmark.State"
    const val modeAnnotationFQN = "kotlinx.benchmark.BenchmarkMode"
    const val timeUnitFQN = "kotlinx.benchmark.BenchmarkTimeUnit"
    //const val iterationTimeFQN = "kotlinx.benchmark.IterationTime"
    const val modeFQN = "kotlinx.benchmark.Mode"
    const val outputTimeAnnotationFQN = "kotlinx.benchmark.OutputTimeUnit"
    const val warmupAnnotationFQN = "kotlinx.benchmark.Warmup"
    const val measureAnnotationFQN = "kotlinx.benchmark.Measurement"
    const val paramAnnotationFQN = "kotlinx.benchmark.Param"

    const val blackholeFQN = "kotlinx.benchmark.Blackhole"

    const val mainBenchmarkPackage = "kotlinx.benchmark.generated"

    val suppressUnusedParameter = AnnotationSpec.builder(Suppress::class).addMember("\"UNUSED_PARAMETER\"").build()
    val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
      "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
    ).build()
  }

  private val executorType = ClassName.bestGuess(platform.executorClass)
  private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)

  val benchmarks = mutableListOf<ClassName>()

  fun generate() {
    processPackage(module, module.getPackage(FqName.ROOT))
    generateRunnerMain()
  }

  private fun generateRunnerMain() {
    val file = FileSpec.builder(mainBenchmarkPackage, "BenchmarkSuite").apply {
      function("main") {
        addAnnotation(optInRuntimeInternalApi)
        val array = ClassName("kotlin", "Array")
        val arrayOfStrings = array.parameterizedBy(WildcardTypeName.producerOf(String::class))
        addParameter("args", arrayOfStrings)
        addStatement("val executor = %T(%S, args)", executorType, title)
        for (benchmark in benchmarks) {
          addStatement("executor.suite(%T.describe())", benchmark)
        }
        addStatement("executor.run()")
      }
    }.build()
    file.writeTo(output)
  }

  private fun processPackage(module: ModuleDescriptor, packageView: PackageViewDescriptor) {
    for (packageFragment in packageView.fragments.filter { it.module == module }) {
      DescriptorUtils.getAllDescriptors(packageFragment.getMemberScope())
        .filterIsInstance<ClassDescriptor>()
        .filter { cls -> cls.annotations.any { it.fqName.toString() == stateAnnotationFQN } }
        .filter { it.modality != Modality.ABSTRACT }
        .forEach {
          generateBenchmark(it)
        }
    }

    for (subpackageName in module.getSubPackagesOf(packageView.fqName, MemberScope.ALL_NAME_FILTER)) {
      processPackage(module, module.getPackage(subpackageName))
    }
  }

  private fun generateBenchmark(original: ClassDescriptor) {
    val originalFqName = original.fqNameSafe
    val originalPackage = originalFqName.parent().let {
      if (it.isRoot) "" else it.asString()
    }
    val originalName = originalFqName.shortName().toString()
    val originalClass = ClassName(originalPackage, originalName)

    val benchmarkPackageName = mainBenchmarkPackage + if (originalPackage.isNotEmpty()) ".$originalPackage" else ""
    val benchmarkName = "${originalName}_Descriptor"
    val benchmarkClass = ClassName(benchmarkPackageName, benchmarkName)

    val functions = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
      .filterIsInstance<FunctionDescriptor>()

    val parameterProperties = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
      .filterIsInstance<PropertyDescriptor>()
      .filter { pd -> pd.annotations.any { it.fqName.toString() == paramAnnotationFQN } }

    validateParameterProperties(parameterProperties)

    val measureAnnotation = original.annotations.singleOrNull { it.fqName.toString() == measureAnnotationFQN }
    val warmupAnnotation = original.annotations.singleOrNull { it.fqName.toString() == warmupAnnotationFQN }
    val outputTimeAnnotation = original.annotations.singleOrNull { it.fqName.toString() == outputTimeAnnotationFQN }
    val modeAnnotation = original.annotations.singleOrNull { it.fqName.toString() == modeAnnotationFQN }

    val outputTimeUnitValue = outputTimeAnnotation?.argumentValue("value") as EnumValue?
    val outputTimeUnit = outputTimeUnitValue?.enumEntryName?.toString()

    @Suppress("UNCHECKED_CAST")
    val modesValue = modeAnnotation?.argumentValue("value")?.value as List<EnumValue>?
    val mode = modesValue?.single()?.enumEntryName?.toString()

    val measureIterations = measureAnnotation?.argumentValue("iterations")?.value as Int?
    val measureIterationTime = measureAnnotation?.argumentValue("time")?.value as Int?
    val measureIterationTimeUnit = measureAnnotation?.argumentValue("timeUnit") as EnumValue?

    val warmupIterations = warmupAnnotation?.argumentValue("iterations")?.value as Int?

    val iterationTimeUnit = measureIterationTimeUnit?.enumEntryName?.toString() ?: "SECONDS"

    val benchmarkFunctions =
      functions.filter { fn -> fn.annotations.any { it.fqName.toString() == benchmarkAnnotationFQN } }

    validateBenchmarkFunctions(benchmarkFunctions)

    val setupFunctions = functions
      .filter { fn -> fn.annotations.any { it.fqName.toString() == setupAnnotationFQN } }

    validateSetupFunctions(setupFunctions)

    val teardownFunctions = functions
      .filter { fn -> fn.annotations.any { it.fqName.toString() == teardownAnnotationFQN } }.reversed()

    validateTeardownFunctions(teardownFunctions)

    val file = FileSpec.builder(benchmarkPackageName, benchmarkName).apply {
      declareObject(benchmarkClass) {
        addAnnotation(suppressUnusedParameter)
        addAnnotation(optInRuntimeInternalApi)

        function(setupFunctionName) {
          addModifiers(KModifier.PRIVATE)
          addParameter("instance", originalClass)
          for (fn in setupFunctions) {
            val functionName = fn.name.toString()
            addStatement("instance.%N()", functionName)
          }
        }

        function(teardownFunctionName) {
          addModifiers(KModifier.PRIVATE)
          addParameter("instance", originalClass)
          for (fn in teardownFunctions) {
            val functionName = fn.name.toString()
            addStatement("instance.%N()", functionName)
          }
        }

        function(parametersFunctionName) {
          addModifiers(KModifier.PRIVATE)
          addParameter("instance", originalClass)
          addParameter("params", MAP.parameterizedBy(STRING, STRING))

          parameterProperties.forEach { property ->
            val type = property.type.nameIfStandardType!!
            addStatement("instance.${property.name} = params.getValue(\"${property.name}\").to$type()")
          }
        }

        val defaultParameters = parameterProperties.associateBy({ it.name }, {
          val annotation = it.annotations.findAnnotation(FqName(paramAnnotationFQN))!!
          @Suppress("UNCHECKED_CAST")
          annotation.argumentValue("value")!!.value as List<StringValue>
        })

        val defaultParametersString = defaultParameters.entries
          .joinToString(separator = ",\n", prefix = "mapOf(\n", postfix = "\n)") { (key, value) ->
            val values = value.joinToString {
              "\"\"\"${it.value.replace(' ', '·')}\"\"\""
            }
            "  \"${key}\" to listOf(${values})"
          }

        val timeUnitClass = ClassName.bestGuess(timeUnitFQN)
        //val iterationTimeClass = ClassName.bestGuess(iterationTimeFQN)
        val modeClass = ClassName.bestGuess(modeFQN)

        function("describe") {
          returns(suiteDescriptorType.parameterizedBy(originalClass))
          addCode(
            """
                          «val descriptor = %T(
                          name = %S,
                          factory = ::%T,
                          setup = ::%N,
                          teardown = ::%N,
                          parametrize = ::%N
                        """.trimIndent(),
            suiteDescriptorType,
            originalName,
            originalClass,
            setupFunctionName,
            teardownFunctionName,
            parametersFunctionName
          )

          val params = parameterProperties
            .joinToString(prefix = "listOf(", postfix = ")") { "\"${it.name}\"" }

          addCode(",\nparameters = $params")

          addCode(",\ndefaultParameters = $defaultParametersString")

          if (measureIterations != null) {
            addCode(",\niterations = $measureIterations")
          }
          if (warmupIterations != null) {
            addCode(",\nwarmups = $warmupIterations")
          }
          if (measureIterationTime != null) {
            addImport("kotlin.time.Duration.Companion", iterationTimeUnit.lowercase())
            addCode(",\nmeasurementDuration = ${measureIterationTime}.${iterationTimeUnit.lowercase()}")
          }
          if (outputTimeUnit != null) {
            addCode(
              ",\noutputTimeUnit = %T.%N", timeUnitClass,
              MemberName(timeUnitClass, outputTimeUnit)
            )
          }
          if (mode != null)
            addCode(
              ",\nmode = %T.%N", modeClass,
              MemberName(modeClass, mode)
            )
          addCode("\n)\n»")
          addStatement("")

          val bhClass = ClassName.bestGuess(blackholeFQN)
          for (fn in benchmarkFunctions) {
            val functionName = fn.name.toString()

            val hasABlackholeParameter = fn.valueParameters.singleOrNull()?.type.toString() == "Blackhole"

            val fqnDescriptorToCreate =
              if (hasABlackholeParameter) platform.benchmarkDescriptorWithBlackholeParameterClass
              else platform.benchmarkDescriptorClass

            addStatement(
              "descriptor.add(%T(%S, descriptor, %T(), %T::%N))",
              ClassName.bestGuess(fqnDescriptorToCreate),
              "${originalClass.canonicalName}.$functionName",
              bhClass,
              originalClass,
              functionName
            )
          }
          addStatement("return descriptor")
        }

      }
      benchmarks.add(benchmarkClass)
    }.build()

    file.writeTo(output)
  }
}

//@KotlinxBenchmarkGeneratorInternalApi
//inline fun codeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
//  return CodeBlock.builder().apply(builderAction).build()
//}

@KotlinxBenchmarkGeneratorInternalApi
inline fun FileSpec.Builder.declareObject(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
  return TypeSpec.objectBuilder(name).apply(builderAction).build().also {
    addType(it)
  }
}

//@KotlinxBenchmarkGeneratorInternalApi
//inline fun FileSpec.Builder.declareClass(name: String, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
//  return TypeSpec.classBuilder(name).apply(builderAction).build().also {
//    addType(it)
//  }
//}

//@KotlinxBenchmarkGeneratorInternalApi
//inline fun FileSpec.Builder.declareClass(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
//  return TypeSpec.classBuilder(name).apply(builderAction).build().also {
//    addType(it)
//  }
//}

@KotlinxBenchmarkGeneratorInternalApi
inline fun TypeSpec.Builder.property(
  name: String,
  type: ClassName,
  builderAction: PropertySpec.Builder.() -> Unit
): PropertySpec {
  return PropertySpec.builder(name, type).apply(builderAction).build().also {
    addProperty(it)
  }
}

@KotlinxBenchmarkGeneratorInternalApi
inline fun TypeSpec.Builder.function(
  name: String,
  builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
  return FunSpec.builder(name).apply(builderAction).build().also {
    addFunction(it)
  }
}

@KotlinxBenchmarkGeneratorInternalApi
inline fun FileSpec.Builder.function(
  name: String,
  builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
  return FunSpec.builder(name).apply(builderAction).build().also {
    addFunction(it)
  }
}

@KotlinxBenchmarkGeneratorInternalApi
val KotlinType.nameIfStandardType: Name?
  get() = constructor.declarationDescriptor?.name
