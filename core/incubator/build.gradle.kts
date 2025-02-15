import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k incubator module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-moshi"))

    api(Square.moshi.adapters)
    implementation(project(mapOf("path" to ":http4k-testing-webdriver")))

    implementation(project(":http4k-realtime-core"))

    compileOnly("org.junit.jupiter:junit-jupiter-api:_")

    testImplementation(project(":http4k-client-apache"))

    testImplementation("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-contract")))
}
