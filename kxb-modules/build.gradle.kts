plugins {
  id("kxb.build.conventions.base")
}

group = "dev.adamko.kotlinx-benchmark-mu"
version = "2.0.0"

val devPublish by tasks.registering {
  group = project.name
//  dependsOn(gradle.includedBuild("kxb-examples").task(":assemble"))
  dependsOn(":kxb-generator:publishAllPublicationsToDevRepoRepository")
  dependsOn(":kxb-gradle-plugin:publishAllPublicationsToDevRepoRepository")
  dependsOn(":kxb-runner:publishAllPublicationsToDevRepoRepository")
  dependsOn(":kxb-runner-parameters:publishAllPublicationsToDevRepoRepository")
}
