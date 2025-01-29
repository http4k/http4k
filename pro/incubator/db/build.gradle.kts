import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k db module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    compileOnly("org.junit.jupiter:junit-jupiter-api:_")
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation(project(":http4k-testing-strikt"))
    api("dev.forkhandles:result4k:_")

    testImplementation("org.hsqldb:hsqldb:_")

    testImplementation("org.postgresql:postgresql:_")

    testImplementation("mysql:mysql-connector-java:_")

    api("com.zaxxer:HikariCP:_")
    api(platform("org.jetbrains.exposed:exposed-bom:_"))
    api(JetBrains.exposed.core)
    api(JetBrains.exposed.jdbc)
}
