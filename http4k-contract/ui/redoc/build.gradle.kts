description = "Add a locally hosted Redoc UI to your server"

dependencies {
    api(project(":http4k-contract"))
    api("org.webjars:redoc:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

