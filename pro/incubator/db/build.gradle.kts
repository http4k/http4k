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

    testImplementation("org.hsqldb:hsqldb:_")

    testImplementation("org.postgresql:postgresql:_")

    testImplementation("mysql:mysql-connector-java:_")

    api("com.zaxxer:HikariCP:_")
    api(platform("org.jetbrains.exposed:exposed-bom:_"))
    api(JetBrains.exposed.core)
    api(JetBrains.exposed.jdbc)
    api("org.jetbrains.exposed:exposed-java-time:_")
}
