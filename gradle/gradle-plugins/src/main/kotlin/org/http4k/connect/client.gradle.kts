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
    ksp(lib("kotshi-compiler"))

    testFixturesApi(lib("kotshi-compiler"))
    testFixturesApi(testFixtures(project(":http4k-connect-core-fake")))
}


fun lib(s: String) = the<VersionCatalogsExtension>()
    .find("libs")
    .flatMap { it.findLibrary(s) }
    .get()
