package org.http4k.connect

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`java-test-fixtures`
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project

plugins {
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-storage-core"))
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
    testFixturesApi(testFixtures(project(":http4k-connect-storage-core")))
}
