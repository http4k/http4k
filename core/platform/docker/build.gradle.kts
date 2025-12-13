

description = "http4k docker integration tooling"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-config"))

    testImplementation(project(":http4k-testing-hamkrest"))
}

