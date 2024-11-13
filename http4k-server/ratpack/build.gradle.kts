description = "Http4k HTTP Server built on top of Ratpack"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api("io.ratpack:ratpack-core:_")

    // to overcome CVEs from outdated ratpack
    api(project(":http4k-format-jackson-yaml"))
    api(project(":http4k-server-netty"))
    api("com.google.guava:guava:_")

    testImplementation(testFixtures(project(":http4k-core")))
}
