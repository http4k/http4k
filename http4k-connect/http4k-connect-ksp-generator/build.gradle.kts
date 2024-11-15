import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-connect-core"))

    implementation( ":http4k-format-moshi")
    implementation("com.squareup:kotlinpoet:_")
    implementation("com.squareup:kotlinpoet-metadata:_")
    implementation("com.squareup:kotlinpoet-ksp:_")
    implementation("com.google.devtools.ksp:symbol-processing-api:_")

    ksp("se.ansman.kotshi:compiler:_")

    testFixturesApi("se.ansman.kotshi:api:_")
    testFixturesApi( ":http4k-format-moshi")
    testFixturesApi("dev.forkhandles:result4k")

    kspTest(project(":http4k-connect-ksp-generator"))
    kspTestFixtures(project(":http4k-connect-ksp-generator"))
}

