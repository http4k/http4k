description = "http4k AI A2A Testing tools"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-a2a-sdk"))
    api(project(":http4k-ai-a2a-client"))

    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-testing-approval"))
}
