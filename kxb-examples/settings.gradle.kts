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
  }
}

includeBuild("../kxb-modules")

//include(":java")
//include(":kotlin")
include(":kotlin-kts")
include(":kotlin-multiplatform")
