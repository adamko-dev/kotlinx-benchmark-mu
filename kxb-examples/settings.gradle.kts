rootProject.name = "kxb-examples"

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
//    maven(file("../build/dev-repo"))
    mavenCentral()
    gradlePluginPortal()

    ivy("https://nodejs.org/dist/") {
      // https://youtrack.jetbrains.com/issue/KT-55620/
      name = "Node Distributions at $url"
      patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
      metadataSources { artifact() }
      content { includeModule("org.nodejs", "node") }
    }
    ivy("https://github.com/yarnpkg/yarn/releases/download") {
      // https://youtrack.jetbrains.com/issue/KT-55620/
      name = "Yarn Distributions at $url"
      patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
      metadataSources { artifact() }
      content { includeModule("com.yarnpkg", "yarn") }
    }
  }
}

includeBuild("../kxb-modules")

//include(":java")
//include(":kotlin")
include(":kotlin-kts")
include(":kotlin-multiplatform")
