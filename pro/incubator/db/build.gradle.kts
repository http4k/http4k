import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k db module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(platform(libs.forkhandles.bom))
    compileOnly(platform(libs.junit.bom))
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation(project(":http4k-testing-strikt"))
    api("dev.forkhandles:result4k")
    api("dev.forkhandles:values4k")
    api("dev.forkhandles:time4k")

    testImplementation(libs.hsqldb)

    testImplementation(libs.postgresql)

    testImplementation(libs.mysql.connector.java)

    api(libs.hikaricp)
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
}
