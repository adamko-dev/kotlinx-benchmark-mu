package kxb.build.conventions

import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing

plugins {
  id("kxb.build.conventions.base")
  `maven-publish`
  //signing
}

publishing {
  repositories {
    val devRepoDir = generateSequence(gradle) { it.parent}.last().rootProject.projectDir.resolve("build/dev-repo")
    maven(devRepoDir) {
      name = "DevRepo"
    }
  }
}
