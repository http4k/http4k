package org.http4k.connect

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

plugins {
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-core-fake"))
    api(project(":${project.name.substring(0, project.name.length - 5)}"))
    testFixturesApi(testFixtures(project(":${project.name.substring(0, project.name.length - 5)}")))
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
}
