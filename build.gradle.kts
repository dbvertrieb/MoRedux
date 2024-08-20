import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version libs.versions.kotlin
    signing
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.axion)
}

// Axion plugin settings
scmVersion {
    versionCreator("versionWithBranch")
}

group = "io.github.dbvertrieb"
version = scmVersion.version
extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockito.inline)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/dbvertrieb/MoRedux")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

mavenPublishing {
    // See https://vanniktech.github.io/gradle-maven-publish-plugin/central/ for documentation of vanniktech maven-publish plugin

    // Publish JavadocJar and sourceJar as well
    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = true,
        )
    )

    // Publishing to https://central.sonatype.com/
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    // Only apply signing when it's a release build
    if (project.extra["isReleaseVersion"] as Boolean) {
        signAllPublications()
    }

    val groupId = group.toString()
    val artifactId = "moredux"
    val version = scmVersion.version

    coordinates(groupId, artifactId, version)

    pom {
        name.set("MoRedux")
        description.set("A Redux inspired library in Kotlin to manage UI state and react on state changes")
        inceptionYear.set("2024")
        url.set("https://github.com/dbvertrieb/moredux")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id = "hbeige"
                name = "Henrik Beige"
                email = "henrik.beige@deutschebahn.com"
            }
        }
        scm {
            url = "https://github.com/dbvertrieb/moredux"
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}