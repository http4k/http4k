description = "Http4k Jackson JSON support"

dependencies {
    api(project(":http4k-format-core"))

    implementation(project(":http4k-format-jackson"))
    implementation(project(":http4k-format-kondor-json"))
    implementation("dev.forkhandles:values4k:_")
    implementation("dev.forkhandles:data4k:_")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(project(":http4k-testing-approval"))
}
