package kxb.build.conventions

import gradle.kotlin.dsl.accessors._07ef6d3f42ada650f23d6e32d0617115.kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("kxb.build.conventions.base")
  kotlin("jvm")
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
