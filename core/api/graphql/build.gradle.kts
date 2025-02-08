import org.http4k.internal.ModuleLicense.Apache2

description = "http4k support for GraphQL"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-jackson"))
    api("com.graphql-java:graphql-java:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

