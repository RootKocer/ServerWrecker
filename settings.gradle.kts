enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("org.cadixdev.licenser") version "0.6.1"
        id("net.kyori.indra") version "3.1.3"
        id("net.kyori.indra.git") version "3.1.3"
        id("net.kyori.indra.checkstyle") version "3.1.3"
        id("net.kyori.blossom") version "2.1.0"
        id("com.google.protobuf") version "0.9.4"
    }
}

plugins {
    id("com.gradle.enterprise") version "3.15.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.opencollab.dev/maven-releases") {
            name = "OpenCollab Releases"
        }
        maven("https://repo.opencollab.dev/maven-snapshots") {
            name = "OpenCollab Snapshots"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        maven("https://repo.viaversion.com/") {
            name = "ViaVersion Repository"
        }
        maven("https://maven.lenni0451.net/releases") {
            name = "Lenni0451"
        }
        maven("https://maven.lenni0451.net/snapshots") {
            name = "Lenni0451 Snapshots"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Repository"
        }
        maven("https://jitpack.io/") {
            name = "JitPack Repository"
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            file("gradle/libs.versions.toml")
        }
    }
}

gradleEnterprise {
    buildScan {
        if (!System.getenv("CI").isNullOrEmpty()) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

include("build-data", "launcher")

rootProject.name = "serverwrecker"
