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
