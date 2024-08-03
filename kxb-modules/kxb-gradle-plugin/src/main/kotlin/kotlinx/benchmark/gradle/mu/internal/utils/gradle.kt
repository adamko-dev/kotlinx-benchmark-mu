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
 * The path of a Gradle [Project][org.gradle.api.Project]. This is unique per subproject.
 * This is _not_ the file path, which
 * [can be configured to be different to the project path](https://docs.gradle.org/current/userguide/fine_tuning_project_layout.html#sub:modifying_element_of_the_project_tree).
 *
 * Example: `:modules:tests:alpha-project`.
 *
 * @see org.gradle.api.Project.getPath
 */
internal typealias GradleProjectPath = org.gradle.util.Path


internal fun Project.pathAsFilePath(): String = path
  .removePrefix(GradleProjectPath.SEPARATOR)
  .replace(GradleProjectPath.SEPARATOR, "/")


/**
 * Apply some configuration to a [Task] using
 * [configure][org.gradle.api.tasks.TaskContainer.configure],
 * and return the same [TaskProvider].
 */
internal fun <T : Task> TaskProvider<T>.configuring(
  block: Action<T>
): TaskProvider<T> = apply { configure(block) }


internal fun <T> NamedDomainObjectContainer<T>.maybeCreate(
  name: String,
  configure: T.() -> Unit,
): T = maybeCreate(name).apply(configure)


/**
 * Aggregate the incoming files from a [Configuration] (with name [named]) into [collector].
 *
 * Configurations that do not exist or cannot be
 * [resolved][org.gradle.api.artifacts.Configuration.isCanBeResolved]
 * will be ignored.
 *
 * @param[builtBy] An optional [TaskProvider], used to set [ConfigurableFileCollection.builtBy].
 * This should not typically be used, and is only necessary in rare cases where a Gradle Plugin is
 * misconfigured.
 */
internal fun ConfigurationContainer.collectIncomingFiles(
  named: String,
  collector: ConfigurableFileCollection,
  builtBy: TaskProvider<*>? = null,
  artifactViewConfiguration: ArtifactView.ViewConfiguration.() -> Unit = {
    // ignore failures: it's usually okay if fetching files is best-effort because
    // maybe Dokka doesn't need _all_ dependencies
    lenient(true)
  },
) {
  val conf = findByName(named)
  if (conf != null && conf.isCanBeResolved) {
    val incomingFiles = conf.incoming
      .artifactView(artifactViewConfiguration)
      .artifacts
      .resolvedArtifacts // using 'resolved' might help with triggering artifact transforms?
      .map { artifacts -> artifacts.map { it.file } }

    collector.from(incomingFiles)

    if (builtBy != null) {
      collector.builtBy(builtBy)
    }
  }
}


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



/**
 * Creates a new [Attribute] of the given name with a type of [String].
 *
 * @see Attribute.of
 */
internal fun Attribute(
  name: String
): Attribute<String> =
  Attribute.of(name, String::class.java)


internal val ArtifactTypeAttribute: Attribute<String> = Attribute("artifactType")


internal fun AttributeContainer.artifactType(value: String) {
  attribute(ArtifactTypeAttribute, value)
}


/**
 * Get all [Attribute]s as a [Map] (helpful for debug printing)
 */
internal fun AttributeContainer.toMap(): Map<Attribute<*>, Any?> =
  keySet().associateWith { getAttribute(it) }


internal fun AttributeContainer.toDebugString(): String =
  toMap().entries.joinToString { (k, v) -> "$k[name:${k.name}, type:${k.type}, type.hc:${k.type.hashCode()}]=$v" }


/**
 * Get an [Attribute] from an [AttributeContainer].
 *
 * (Nicer Kotlin accessor function).
 */
internal operator fun <T : Any> AttributeContainer.get(key: Attribute<T>): T? {
  // first, try the official way
  val value = getAttribute(key)
  if (value != null) {
    return value
  }

  // Failed to get attribute using official method, which might have been caused by a Gradle bug
  // https://github.com/gradle/gradle/issues/28695
  // Attempting to check...

  // Quickly check that any attribute has the same name.
  // (There's no point in checking further if no names match.)
  if (keySet().none { it.name == key.name }) {
    return null
  }

  val actualKey = keySet()
    .firstOrNull { candidate -> candidate.matchesTypeOf(key) }
    ?: return null

  error(
    """
      Gradle failed to fetch attribute from AttributeContainer, even though the attribute is present.
      Please report this error to Gradle https://github.com/gradle/gradle/issues/28695
        Requested attribute: $key ${key.type} ${key.type.hashCode()}
        Actual attribute: $actualKey ${actualKey.type} ${actualKey.type.hashCode()}
        All attributes: ${toDebugString()}
        Gradle Version: $CurrentGradleVersion
    """.trimIndent()
  )
}

/** Leniently check if [Attribute.type]s are equal, avoiding [Class.hashCode] classloader differences. */
private fun Attribute<*>.matchesTypeOf(other: Attribute<*>): Boolean {
  val thisTypeId = this.typeId() ?: false
  val otherTypeId = other.typeId() ?: false
  return thisTypeId == otherTypeId
}

/**
 * An ID for [Attribute.type] that is stable across different classloaders.
 *
 * Workaround for https://github.com/gradle/gradle/issues/28695.
 */
private fun Attribute<*>.typeId(): String? =
  type.toString().ifBlank { null }
