plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "MoRedux"

// version catalog
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.2.21")
            version("axion", "1.21.1")
            version("vanniktech-publish", "0.29.0")
            version("kotlinx-coroutines", "1.10.2")
            version("junit-jupiter", "6.0.1")
            version("google-truth", "1.4.5")
            version("mockito", "5.2.0")
            version("ben-manes", "0.51.0")

            plugin("vanniktech-publish", "com.vanniktech.maven.publish").versionRef("vanniktech-publish")
            plugin("axion", "pl.allegro.tech.build.axion-release").versionRef("axion")
            plugin("ben-manes", "com.github.ben-manes.versions").versionRef("ben-manes")

            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("kotlinx-coroutines")

            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit-jupiter")
            library("google-truth", "com.google.truth", "truth").versionRef("google-truth")
            library("mockito-inline", "org.mockito", "mockito-inline").versionRef("mockito")
        }
    }
}
