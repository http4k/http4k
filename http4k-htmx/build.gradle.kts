description = "Http4k htmx support utilities"

dependencies {
    api(project(":http4k-core"))
    api("org.webjars.npm:htmx.org:_")
    api("org.webjars.npm:hyperscript.org:_")
    api("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
}
