rootProject.name = "kxb-modules"

pluginManagement {
  includeBuild("../build-logic/build-plugins")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}


@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_SETTINGS

  repositories {
    mavenCentral()
    gradlePluginPortal() // google.d8:v8:mac-arm64-rel-11.9.85

    //region workaround for https://youtrack.jetbrains.com/issue/KT-51379
    // FIXME remove when updating to Kotlin 2.0
    ivy("https://download.jetbrains.com/kotlin/native/builds") {
      name = "KotlinNative"
      patternLayout {
        listOf(
          "macos-x86_64",
          "macos-aarch64",
          "osx-x86_64",
          "osx-aarch64",
          "linux-x86_64",
          "windows-x86_64",
        ).forEach { os ->
          listOf("dev", "releases").forEach { stage ->
            artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
          }
        }
      }
      content { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
      metadataSources { artifact() }
    }
    //endregion

    //region Declare the Node.js & Yarn download repositories
    // Workaround https://youtrack.jetbrains.com/issue/KT-68533/
    ivy("https://nodejs.org/dist/") {
      name = "Node Distributions at $url"
      patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
      metadataSources { artifact() }
      content { includeModule("org.nodejs", "node") }
    }
    ivy("https://github.com/yarnpkg/yarn/releases/download") {
      name = "Yarn Distributions at $url"
      patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
      metadataSources { artifact() }
      content { includeModule("com.yarnpkg", "yarn") }
    }
    //endregion
  }

  versionCatalogs.create("libs") {
    from(files("../gradle/libs.versions.toml"))
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

include(":kxb-gradle-plugin")
include(":kxb-generator")
include(":kxb-runner")
include(":kxb-runner-parameters")
