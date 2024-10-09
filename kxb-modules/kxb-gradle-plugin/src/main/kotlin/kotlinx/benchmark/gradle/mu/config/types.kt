package kotlinx.benchmark.gradle.mu.config

import kotlinx.benchmark.gradle.mu.internal.utils.domainObjectContainer
import kotlinx.benchmark.gradle.mu.internal.utils.polymorphicDomainObjectContainer
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

/** Container for [BenchmarkTarget]s. */
typealias BenchmarkTargetsContainer =
    ExtensiblePolymorphicDomainObjectContainer<BenchmarkTarget>

/** Container for [BenchmarkRunSpec]s. */
typealias BenchmarkRunSpecsContainer =
    NamedDomainObjectContainer<BenchmarkRunSpec>

/** Create a new [BenchmarkTargetsContainer] instance. */
internal fun ObjectFactory.benchmarkTargetsContainer(): BenchmarkTargetsContainer {
  val container = polymorphicDomainObjectContainer<BenchmarkTarget>()
  container.whenObjectAdded {
    // workaround for https://github.com/gradle/gradle/issues/24972
    (container as ExtensionAware).extensions.add(name, this)
  }
  return container
}

/** Create a new [BenchmarkRunSpecsContainer] instance. */
internal fun ObjectFactory.benchmarkRunSpecsContainer(): BenchmarkRunSpecsContainer {
  return domainObjectContainer<BenchmarkRunSpec>()
//  container.whenObjectAdded {
//    // workaround for https://github.com/gradle/gradle/issues/24972
//    (container as ExtensionAware).extensions.add(name, this)
//  }
//  return container
}
