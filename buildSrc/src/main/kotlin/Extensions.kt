import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

val BUILD_NUMBER: String? = System.getenv("BUILD_NUMBER")

val SUPPORTED_MINECRAFT_VERSION = listOf(
    "1.21.4"
)

val Project.libs
    get() = rootProject.extensions.getByName("libs") as LibrariesForLibs

val Project.manifestAttribute get() = mapOf(
    "Dev-Build" to (BUILD_NUMBER != null),
    "Version" to property("version"),
    "Author" to "toxicity188",
    "Url" to "https://github.com/toxicity188/BetterHud",
    "Created-By" to "Gradle $gradle",
    "Build-Jdk" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
    "Build-OS" to "${System.getProperty("os.arch")} ${System.getProperty("os.name")}"
)
