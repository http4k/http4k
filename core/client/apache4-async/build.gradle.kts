

description = "http4k HTTP Client built on top of async apache httpclient"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.apache.httpasyncclient)
    testImplementation(testFixtures(project(":http4k-core")))
}
