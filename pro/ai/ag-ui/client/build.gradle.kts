description = "http4k AI AG-UI Client support"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-ag-ui-core"))
    api(project(":http4k-realtime-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-approval"))
}
