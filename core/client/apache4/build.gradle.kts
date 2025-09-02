import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Client built on top of apache-httpclient"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.apache.httpclient)
    testImplementation(testFixtures(project(":http4k-core")))
}
