package org.http4k.connect

description = "http4k Connect Storage: ${project.name.removePrefix("http4k-connect-storage-")}"

plugins {
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-storage-core"))
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
    testFixturesApi(testFixtures(project(":http4k-connect-storage-core")))
}
