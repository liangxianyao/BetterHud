plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "kr.toxicity.hud"
version = property("version").toString() + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

val targetJavaVersion = 21

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    api(libs.betterCommand)
}

tasks {
    test {
        useJUnitPlatform()
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

dokka {
    moduleName = project.name
    dokkaSourceSets.configureEach {
        displayName = project.name
    }
}
