package org.http4k.connect

description = "http4k Connect Fake: ${project.name.removePrefix("http4k-connect-").removeSuffix("-fake")}"

plugins {
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-core-fake"))
    api(project(":${project.name.substring(0, project.name.length - 5)}"))
    testFixturesApi(testFixtures(project(":${project.name.substring(0, project.name.length - 5)}")))
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
}
