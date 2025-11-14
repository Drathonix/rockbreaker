plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.1.0"
}

repositories {
    maven("https://maven.kikugie.dev/snapshots")
    maven("https://maven.kikugie.dev/releases")
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.7.10")
}

gradlePlugin {
    plugins {
        register("rockbreaker-plugin") {
            id = "rockbreaker"
            implementationClass = "com.drathonix.rockbreakerplugin.RockbreakerPlugin"
        }
    }
}

repositories {
    jcenter()
}