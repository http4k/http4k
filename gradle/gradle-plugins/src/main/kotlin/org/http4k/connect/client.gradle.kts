package org.http4k.connect

import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("org.http4k.connect.module")
    id("com.google.devtools.ksp")
}

tasks {
    withType<KspTask> {
        outputs.upToDateWhen { false }
    }
}

dependencies {
    api(project(":http4k-connect-core"))
    api(project(":http4k-config"))
    ksp(project(":http4k-connect-ksp-generator"))
    ksp("se.ansman.kotshi:compiler:4.0.0")

    testFixturesApi("se.ansman.kotshi:compiler:4.0.0")
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
}
