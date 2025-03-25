import dev.kikugie.stonecutter.settings.StonecutterSettings

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/maven")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
    plugins {
        kotlin("jvm") version "1.9.24"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6-alpha.11"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
extensions.configure<StonecutterSettings> {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        versions("1.20.1")
        branch("common") {
            versions("1.20.1")
        }
        branch("fabric") {
            versions("1.20.1")
        }
        branch("forge") {
            versions("1.20.1")
        }
        branch("neoforge"){
            versions(
                "1.20.6",
                "1.21.1",
                "1.21.2",
                "1.21.4"
            )
        }
        // The API should change minimally, add any relevant versions here to update.
        branch("api"){
            versions("1.20.1")
        }
        vcsVersion="1.20.1"
    }
    create(rootProject)
}

// *** Replace this with your project name
rootProject.name = "rockbreaker"
include("neoforge")
include("forge")
include("fabric")
include("common")
include("api")




