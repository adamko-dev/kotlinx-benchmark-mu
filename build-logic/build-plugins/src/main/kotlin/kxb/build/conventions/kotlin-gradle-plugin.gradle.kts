package kxb.build.conventions

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("kxb.build.conventions.base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("kxb.build.conventions.publishing")
}

tasks.validatePlugins {
  enableStricterValidation = true
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    jvmTarget = JvmTarget.JVM_1_8
    freeCompilerArgs.add("-Xjdk-release=1.8")
  }
}

tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_1_8
    freeCompilerArgs.add("-Xjdk-release=1.8")
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 8
}
