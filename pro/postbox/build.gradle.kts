

description = "http4k Transaactional Postbox"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(platform(libs.forkhandles.bom))
    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.api)
    api(project(":http4k-core"))

    testImplementation(testFixtures(project(":http4k-core")))

    api(libs.result4k)
    api(libs.values4k)
    api(libs.time4k)
    api(libs.tx4k.jdbc)

    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(libs.hsqldb)
    testImplementation(libs.postgresql)
    testImplementation(libs.mysql.connector.java)

    api(libs.hikaricp)
}
