description = "Http4k htmx support utilities"

dependencies {
    api(project(":http4k-core"))
    api("org.webjars.npm:htmx.org:_")
    api("dev.forkhandles:values4k:_")

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
