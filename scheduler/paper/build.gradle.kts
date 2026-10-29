plugins {
    alias(libs.plugins.conventions.standard)
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
}
