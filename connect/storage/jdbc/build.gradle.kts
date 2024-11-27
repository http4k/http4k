import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api("org.jetbrains.exposed:exposed-core:_")
    api("org.jetbrains.exposed:exposed-jdbc:_")

    testFixturesApi("com.zaxxer:HikariCP:_")
    testFixturesApi("com.h2database:h2:_")
}
