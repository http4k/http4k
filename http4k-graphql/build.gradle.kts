description = "Http4k GraphQL support"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-jackson"))
    api("com.graphql-java:graphql-java:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
