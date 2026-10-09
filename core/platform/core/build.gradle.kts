

description = "http4k Cloud core"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-config"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
