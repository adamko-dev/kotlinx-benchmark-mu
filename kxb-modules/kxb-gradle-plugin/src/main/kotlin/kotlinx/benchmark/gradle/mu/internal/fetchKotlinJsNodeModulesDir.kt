package kotlinx.benchmark.gradle.mu.internal

import java.io.File
import kotlinx.benchmark.gradle.mu.internal.utils.consumable
import kotlinx.benchmark.gradle.mu.internal.utils.declarable
import kotlinx.benchmark.gradle.mu.internal.utils.resolvable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project


// workaround non-idiomatic Kotlin/JS Gradle code

internal fun fetchKotlinJsNodeModulesDir(project: Project): Provider<File> {
  val objects = project.objects

  configureRootProject(project.rootProject)

  val kotlinJsNodeModules: Configuration by project.configurations.creating {
    description = "Depend on the Kotlin/JS node_modules directory from the root project."

    declarable()

    defaultDependencies {
      add(project.dependencies.project(":"))
    }
  }

  val kotlinJsNodeModulesResolver: Configuration by project.configurations.creating {
    description = "Resolves ${kotlinJsNodeModules.name}."

    resolvable()

    extendsFrom(kotlinJsNodeModules)

    attributes {
      attribute(CATEGORY_ATTRIBUTE, objects.kotlinJsNodeModulesCategory)
    }
  }

  return kotlinJsNodeModulesResolver.incoming.files.elements.map { it.single().asFile }
}


private fun configureRootProject(rootProject: Project) {
  val layout = rootProject.layout
  val objects = rootProject.objects

  rootProject.configurations.maybeCreate("kotlinJsNodeModulesOutgoing").apply {
    description = "Provide the Kotlin/JS node_modules directory."

    consumable()

    attributes {
      attribute(CATEGORY_ATTRIBUTE, objects.kotlinJsNodeModulesCategory)
    }

    outgoing {
      artifact(layout.buildDirectory.dir("js/node_modules")) {
        builtBy(":kotlinNpmInstall")
      }
    }
  }
}

private val ObjectFactory.kotlinJsNodeModulesCategory: Category
  get() = named("kotlin-js-node-modules")
