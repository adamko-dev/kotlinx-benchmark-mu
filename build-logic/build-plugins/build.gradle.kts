import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
//  idea
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation(libs.kotlin.gradlePlugin)
}

//idea {
//  module {
//    excludeDirs.plusAssign(
//      listOf(
//        ".idea",
//        ".kotlin",
//      ).map(::File)
//    )
//    excludeDirs.plusAssign(
//      listOf(
//        "kotlin-dsl-accessors",
//        "kotlin-dsl-external-plugin-spec-builders",
//        "kotlin-dsl-plugins",
//      ).map { File("./build/generated-sources/$it/kotlin/gradle/") }
//    )
//  }
//}


////region disable 'Unsupported Kotlin plugin version' warning
//val disableUnsupportedKotlinPluginVersionWarning by tasks.registering {
//  description = "Workaround for https://github.com/gradle/gradle/issues/13020"
//
//  val embeddedKotlinVersion = embeddedKotlinVersion
//  inputs.property("embeddedKotlinVersion", embeddedKotlinVersion)
//
//  outputs.dir(temporaryDir).withPropertyName("outputDir")
//
//  doLast {
//    temporaryDir.resolve("project.properties").apply {
//      parentFile.mkdirs()
//      writeText(
//        """
//        project.version=$embeddedKotlinVersion
//        kotlin.native.version=$embeddedKotlinVersion
//        """.trimIndent()
//      )
//    }
//  }
//}
//
//sourceSets {
//  main {
//    resources.srcDir(disableUnsupportedKotlinPluginVersionWarning)
//  }
//}
////endregion
