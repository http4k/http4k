import org.http4k.internal.ModuleLicense.Apache2

description = "http4k YAML support using Jackson as an underlying engine"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api(platform("com.fasterxml.jackson:jackson-bom:_"))
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-format-jackson")))
}
