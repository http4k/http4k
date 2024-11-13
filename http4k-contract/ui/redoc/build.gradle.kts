description = "Add a locally hosted Redoc UI to your server"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-contract"))
    api("org.webjars:redoc:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

