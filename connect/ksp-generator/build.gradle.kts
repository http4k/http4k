import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-connect-core"))

    api(project(":http4k-format-moshi"))
    api("com.squareup:kotlinpoet:_")
    api("com.squareup:kotlinpoet-metadata:_")
    api("com.squareup:kotlinpoet-ksp:_")
    api("com.google.devtools.ksp:symbol-processing-api:_")

    ksp("se.ansman.kotshi:compiler:_")

    testFixturesApi("se.ansman.kotshi:api:_")
    testFixturesApi(project(":http4k-format-moshi"))
    testFixturesApi("dev.forkhandles:result4k")

    kspTest(project(":http4k-connect-ksp-generator"))
    kspTestFixtures(project(":http4k-connect-ksp-generator"))
}

