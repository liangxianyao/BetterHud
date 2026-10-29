pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Standard
        maven("https://repo.papermc.io/repository/maven-public/") //Paper
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") //Spigot
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://jitpack.io")

        // Bukkit
        maven("https://maven.enginehub.org/repo/") //WorldEdit, WorldGuard
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/") //MMOItems, MMOCore, MythicLib
        maven("https://repo.skriptlang.org/releases") //Skript
        maven("https://repo.alessiodp.com/releases/") //Parties
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") //PlaceholderAPI
        maven("https://mvn.lumine.io/repository/maven/") //MythicMobs
        maven("https://repo.momirealms.net/releases/") //CraftEngine
        maven("https://repo.nexomc.com/releases/") //Nexo

    }
}

rootProject.name = "BetterHud"

include(
    "api:bukkit-api",
    "dist",
    "nms:v1_21_R3",
    "scheduler:standard",
    "scheduler:paper",
    "bedrock:geyser",
    "bedrock:floodgate",
    "bootstrap:bukkit"
)
