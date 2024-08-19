import java.net.URI

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
    signing
    id("pl.allegro.tech.build.axion-release") version libs.versions.axion
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

java {
    // Additionally create javadoc and sources JAR upon publish
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "moredux"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = artifactId
                description = "A Redux library in Kotlin to manage UI state and react on state changes"
                url = "https://github.com/dbvertrieb/moredux"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
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
                    url = "https://github.com/dbvertrieb/MoRedux"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/dbvertrieb/MoRedux")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "OSSRH"
            url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

tasks.withType<Sign>().configureEach {
    // Skip signing completely, if the current build is not a release version
    onlyIf("isReleaseVersion is set") { project.extra["isReleaseVersion"] as Boolean }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}