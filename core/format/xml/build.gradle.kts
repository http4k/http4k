import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k XML support using GSON as an underlying engine"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-format-gson"))
    api("org.json:json:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
