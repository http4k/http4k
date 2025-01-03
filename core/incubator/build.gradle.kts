import org.http4k.internal.ModuleLicense.Apache2

description = "http4k incubator module"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases")
    }
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-realtime-core"))

    api(Square.moshi.adapters)
    implementation(project(mapOf("path" to ":http4k-testing-webdriver")))

    api("org.gradle:gradle-tooling-api:${gradle.gradleVersion}")

    compileOnly("org.junit.jupiter:junit-jupiter-api:_")

    testImplementation(project(":http4k-client-apache"))

    testImplementation("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-contract")))
}
