package kotlinx.benchmark.gradle.mu.internal.adapters

import javax.inject.Inject
import kotlinx.benchmark.gradle.mu.BenchmarkExtension
import kotlinx.benchmark.gradle.mu.BenchmarkPlugin
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

internal abstract class KxbJavaAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  private val logger = Logging.getLogger(this::class.java)

  override fun apply(project: Project) {
    logger.info("Applying KxbJavaAdapter to ${project.path}")

    project.plugins.withType<BenchmarkPlugin>().configureEach {
      val kxbExtension = project.extensions.getByType<BenchmarkExtension>()

      // wait for the Java plugin to be applied
//      project.plugins.withType<JavaBasePlugin>().configureEach {
//        handleJavaPlugin(project, kxbExtension)
//      }
    }
  }

  private fun handleJavaPlugin(
    project: Project,
    kxbExtension: BenchmarkExtension,
  ) {
    val java = project.extensions.getByType<JavaPluginExtension>()
//    val sourceSets = project.extensions.getByType<SourceSetContainer>()

//    val isConflictingPluginPresent = isConflictingPluginPresent(project)

    java.sourceSets.configureEach {
      kxbExtension.targets.register<BenchmarkTarget.Java>(name) {
//        enabled.set(!isConflictingPluginPresent)
      }
    }
  }


//  /**
//   * The Android and Kotlin plugins _also_ add the Java plugin.
//   *
//   * To prevent generating documentation for the same sources twice, automatically
//   * disable any TODO ...
//   * when any Android or Kotlin plugin is present
//   *
//   * Projects with Android or Kotlin projects present will be handled by
//   * TODO ...
//   */
//  private fun isConflictingPluginPresent(
//    project: Project
//  ): Provider<Boolean> {
//
//    val projectHasKotlinPlugin = providers.provider {
//      project.pluginManager.hasPlugin(PluginId.KotlinAndroid)
//          || project.pluginManager.hasPlugin(PluginId.KotlinJs)
//          || project.pluginManager.hasPlugin(PluginId.KotlinJvm)
//          || project.pluginManager.hasPlugin(PluginId.KotlinMultiplatform)
//    }
//
//    val projectHasAndroidPlugin = providers.provider {
//      project.pluginManager.hasPlugin(PluginId.AndroidBase)
//          || project.pluginManager.hasPlugin(PluginId.AndroidApplication)
//          || project.pluginManager.hasPlugin(PluginId.AndroidLibrary)
//    }
//
//    return projectHasKotlinPlugin or projectHasAndroidPlugin
//  }

  internal companion object
}
