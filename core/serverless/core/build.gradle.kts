

description = "http4k Serverless core"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
}
