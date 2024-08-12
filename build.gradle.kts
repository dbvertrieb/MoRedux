plugins {
    kotlin("jvm") version "2.0.0"
}

group = "de.db.moredux"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
// TODO version catalog
dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}