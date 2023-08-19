description = "Http4k HTMX support utilities"

dependencies {
    api(project(":http4k-core"))
    api("org.webjars.npm:htmx.org:_")

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
