

description = "http4k HTTP Client built on top of async apache httpclient"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.apache.httpasyncclient)
    testImplementation(testFixtures(project(":http4k-core")))
}
