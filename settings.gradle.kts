plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "MoRedux"

// version catalog
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.0")
            version("axion", "1.18.3")
            version("kotlinx-coroutines", "1.8.1")
            version("junit-jupiter", "5.10.0")
            version("google-truth", "1.4.4")
            version("mockito", "5.2.0")

            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("kotlinx-coroutines")

            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit-jupiter")
            library("google-truth", "com.google.truth", "truth").versionRef("google-truth")
            library("mockito-inline", "org.mockito", "mockito-inline").versionRef("mockito")
        }
    }
}
