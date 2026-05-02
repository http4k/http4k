description = "http4k AI A2A server SDK"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-a2a-core"))
    implementation(project(":http4k-wiretap"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-ai-a2a-client"))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-testing-approval"))
}
