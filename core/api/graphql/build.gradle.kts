

description = "http4k support for GraphQL"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-jackson"))
    api(libs.graphql.java)
    testImplementation(testFixtures(project(":http4k-core")))
}

