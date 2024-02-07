description = "Http4k Jackson JSON support"

dependencies {
    api(project(":http4k-format-core"))

    implementation(project(":http4k-format-jackson"))
    implementation("dev.forkhandles:values4k:2.13.4.0")
    implementation("dev.forkhandles:data4k:2.13.4.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(project(":http4k-testing-approval"))
}
