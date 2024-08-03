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
    mavenCentral()
    gradlePluginPortal()
  }
}

includeBuild("../kxb-modules")

//include(":java")
//include(":kotlin")
include(":kotlin-kts")
//include(":kotlin-multiplatform")
