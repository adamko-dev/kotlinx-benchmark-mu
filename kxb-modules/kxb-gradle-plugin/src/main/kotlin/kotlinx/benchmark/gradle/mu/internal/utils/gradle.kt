package kotlinx.benchmark.gradle.mu.internal.utils

import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
import org.gradle.util.GradleVersion


/** Shortcut for [GradleVersion.current] */
internal val CurrentGradleVersion: GradleVersion
  get() = GradleVersion.current()


/** Compare a [GradleVersion] to a [version]. */
internal operator fun GradleVersion.compareTo(version: String): Int =
  compareTo(GradleVersion.version(version))


///** Only matches components that come from subprojects */
//internal object LocalProjectOnlyFilter : Spec<ComponentIdentifier> {
//  override fun isSatisfiedBy(element: ComponentIdentifier?): Boolean =
//    element is ProjectComponentIdentifier
//}


/** Invert the result of a [Spec] predicate */
internal operator fun <T> Spec<T>.not(): Spec<T> = Spec<T> { !this@not.isSatisfiedBy(it) }


/**
 * Create a new [NamedDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.domainObjectContainer]
 * (but [T] is `reified`).
 *
 * @param[factory] an optional factory for creating elements
 * @see org.gradle.kotlin.dsl.domainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.domainObjectContainer(
  factory: NamedDomainObjectFactory<T>? = null
): NamedDomainObjectContainer<T> =
  if (factory == null) {
    domainObjectContainer(T::class)
  } else {
    domainObjectContainer(T::class, factory)
  }


/**
 * Create a new [ExtensiblePolymorphicDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.polymorphicDomainObjectContainer]
 * (but [T] is `reified`).
 *
 * @see org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.polymorphicDomainObjectContainer()
    : ExtensiblePolymorphicDomainObjectContainer<T> =
  polymorphicDomainObjectContainer(T::class)


/**
 * Add an extension to the [ExtensionContainer], and return the value.
 *
 * Adding an extension is especially useful for improving the DSL in build scripts when [T] is a
 * [NamedDomainObjectContainer].
 * Using an extension will allow Gradle to generate
 * [type-safe model accessors](https://docs.gradle.org/current/userguide/kotlin_dsl.html#kotdsl:accessor_applicability)
 * for added types.
 *
 * ([name] should match the property name. This has to be done manually. I tried using a
 * delegated-property provider, but then Gradle can't introspect the types properly, so it fails to
 * create accessors.)
 */
internal inline fun <reified T : Any> ExtensionContainer.adding(
  name: String,
  value: T,
): T {
  add<T>(name, value)
  return value
}


///**
// * Creates a new [Attribute] of the given name with a type of [String].
// *
// * @see Attribute.of
// */
//internal fun Attribute(
//  name: String
//): Attribute<String> =
//  Attribute.of(name, String::class.java)


/** Drop the first [count] directories from the path */
internal fun RelativePath.dropDirectories(count: Int): RelativePath =
  RelativePath(true, *segments.drop(count).toTypedArray())
