import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Server built on top of Ratpack"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-bridge-ratpack"))

    testImplementation(testFixtures(project(":http4k-core")))
}
