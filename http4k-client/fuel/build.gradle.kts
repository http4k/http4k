description = "HTTP Client built on top of fuel"

dependencies {
    api(project(":http4k-core"))
    api("com.github.kittinunf.fuel:fuel:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
