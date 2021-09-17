description = "Http4k Moshi JSON support"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.squareup.moshi:moshi:_")
    api("com.squareup.moshi:moshi-kotlin:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration ="testArtifacts"))
}
