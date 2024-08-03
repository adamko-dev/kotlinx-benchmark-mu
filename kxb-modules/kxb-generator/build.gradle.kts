plugins {
  id("kxb.build.conventions.kotlin-jvm")
  id("kxb.build.conventions.publishing")
}

dependencies {
  implementation(libs.squareup.kotlinpoet)
  implementation(libs.kotlin.compilerEmbeddable)
  implementation(libs.jmh.generatorBytecode)
}
