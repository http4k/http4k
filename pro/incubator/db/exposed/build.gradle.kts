description = "http4k db Exposed module"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-incubator-db-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-incubator-db-core")))

    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    api(platform(libs.forkhandles.bom))
    api(libs.result4k)
    api(libs.values4k)
    api(libs.time4k)

    testImplementation(libs.hsqldb)
    testImplementation(libs.postgresql)
    testImplementation(libs.mysql.connector.java)

    api(libs.hikaricp)
}
