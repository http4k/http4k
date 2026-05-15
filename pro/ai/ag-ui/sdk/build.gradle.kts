description = "http4k AI AG-UI server SDK"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-ag-ui-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-ai-ag-ui-client"))
    testImplementation(project(":http4k-testing-approval"))
}
