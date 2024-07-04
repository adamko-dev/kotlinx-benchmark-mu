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
    gradlePluginPortal()
  }

  versionCatalogs.create("libs") {
    from(files("../gradle/libs.versions.toml"))
  }
}

include(":kxb-runtime")
include(":kxb-gradle-plugin")
include(":kxb-generator")
