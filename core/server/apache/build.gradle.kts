import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Server built on top of Apache httpcore"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.apache.httpcore5)
    testImplementation(testFixtures(project(":http4k-core")))
}
