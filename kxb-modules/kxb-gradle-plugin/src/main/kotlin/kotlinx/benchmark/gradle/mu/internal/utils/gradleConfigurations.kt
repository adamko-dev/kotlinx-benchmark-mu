package kotlinx.benchmark.gradle.mu.internal.utils

import org.gradle.api.artifacts.Configuration


/** Mark this [Configuration] as one that will be used to declare dependencies. */
internal fun Configuration.declarable() {
  isCanBeDeclaredCompat = true
  isCanBeResolved = false
  isCanBeConsumed = false
  isVisible = false
}

/** Mark this [Configuration] as one that will be used to resolve dependencies. */
internal fun Configuration.resolvable() {
  isCanBeDeclaredCompat = false
  isCanBeResolved = true
  isCanBeConsumed = false
  isVisible = false
}

@Suppress("UnstableApiUsage")
/** `true` if [Configuration.isCanBeDeclared] is supported by the current Gradle version. */
private val isCanBeDeclaredSupported = CurrentGradleVersion >= "8.2"

// Remove when minimum supported Gradle version is >= 8.2
@Suppress("UnstableApiUsage")
private var Configuration.isCanBeDeclaredCompat: Boolean
  get() = if (isCanBeDeclaredSupported) isCanBeDeclared else false
  set(value) {
    if (isCanBeDeclaredSupported) isCanBeDeclared = value
  }
