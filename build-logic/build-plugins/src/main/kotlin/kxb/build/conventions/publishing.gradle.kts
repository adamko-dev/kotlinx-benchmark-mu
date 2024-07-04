package kxb.build.conventions

import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing

plugins {
  id("kxb.build.conventions.base")
  `maven-publish`
  signing
}
