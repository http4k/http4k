description = "Http4k Security OAuth support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module= "kotlin-reflect")
    }
    api("dev.forkhandles:result4k:_")
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
