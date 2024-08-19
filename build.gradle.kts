import java.net.URI

plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    // TODO reenable signing
//    signing
    id("pl.allegro.tech.build.axion-release") version "1.18.3"
}

// Axion plugin settings
scmVersion {
    versionCreator("versionWithBranch")
}

group = "io.github.dbvertrieb"
version = scmVersion.version

repositories {
    mavenCentral()
}
// TODO version catalog
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
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
    }
}
// TODO reenable signing
//signing {
//    sign(publishing.publications["mavenJava"])
//}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}