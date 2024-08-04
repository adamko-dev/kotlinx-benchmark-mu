package kxb.build.conventions

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("kxb.build.conventions.base")
  kotlin("multiplatform")
}

kotlin {
  jvmToolchain(21)
  targets.withType<KotlinJvmTarget>().configureEach {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget = JvmTarget.JVM_1_8
      freeCompilerArgs.add("-Xjdk-release=1.8")
    }
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
