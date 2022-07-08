description = "http4k typesafe HTTP contracts and OpenApi support"

dependencies {
    api(project(":http4k-core"))
    api("org.jetbrains.kotlin:kotlin-reflect:_")

    implementation("dev.forkhandles:values4k:_")
    implementation(project(":http4k-security-oauth"))
    implementation(project(":http4k-format-jackson"))

    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-format-argo"))
    testImplementation(project(":http4k-multipart"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-security-oauth", configuration = "testArtifacts"))
}
