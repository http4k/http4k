description = "HTTP Client built on top of fuel"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api("com.github.kittinunf.fuel:fuel:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
