package kotlinx.benchmark.gradle.mu.tooling

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

data class Platform(
  val arch: Arch,
  val system: System,
) {
  enum class Arch {
    X64,
    Arm64,
    Ppc64LE,
    S390x,
    X86,
  }

  enum class System {
    Linux,
    MacOS,
    Windows,
    SunOS,
  }

  companion object {
    internal fun getHostPlatform(
      providers: ProviderFactory
    ): Provider<Platform> {
      return providers.zip(
        getHostSystem(providers),
        getHostArch(providers),
      ) { system, arch ->
        Platform(arch, system)
      }
    }
  }
}


private fun getHostSystem(
  providers: ProviderFactory
): Provider<Platform.System> {
  return providers.systemProperty("os.name")
    .map { it.lowercase() }
    .map { osName ->
      when {
        "windows" in osName -> Platform.System.Windows
        "mac" in osName     -> Platform.System.MacOS
        "linux" in osName   -> Platform.System.Linux
        "freebsd" in osName -> Platform.System.Linux
        "sunos" in osName   -> Platform.System.SunOS
        else                -> error("Unsupported OS: $osName")
      }
    }
}

private fun getHostArch(providers: ProviderFactory): Provider<Platform.Arch> {
  return providers.systemProperty("os.arch")
    .map { it.lowercase() }
    .map { osArch ->
      when {
        osArch == "arm" || osArch.startsWith("aarch") -> {
          when (
            val uname = uname(providers).get().lowercase()
          ) {
            "armv8l", "aarch64" -> Platform.Arch.Arm64

            "x86_64"            -> Platform.Arch.X64

            else                -> Platform.Arch.values().firstOrNull { it.name.lowercase() == uname }
              ?: error("Could not determine arch for arch:$osArch, uname:$uname")
          }
        }

        osArch == "ppc64le"                           -> Platform.Arch.Ppc64LE
        osArch == "s390x"                             -> Platform.Arch.S390x
        "64" in osArch                                -> Platform.Arch.X64
        else                                          -> Platform.Arch.X86
      }
    }
}

private fun uname(providers: ProviderFactory): Provider<String> {
  @Suppress("UnstableApiUsage")
  return providers.exec {
    executable = "uname"
    args("-m")
    isIgnoreExitValue = true
  }.standardOutput.asText.map { it.trim() }
}
