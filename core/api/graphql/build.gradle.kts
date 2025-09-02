import org.http4k.internal.ModuleLicense.Apache2

description = "http4k support for GraphQL"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-jackson"))
    api(libs.graphql.java)
    testImplementation(testFixtures(project(":http4k-core")))
}

