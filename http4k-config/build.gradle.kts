description = "Machinery for configuring Http4k apps in a typesafe way"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-jackson-yaml"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
