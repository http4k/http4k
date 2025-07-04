import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Moshi JSON support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(Square.moshi)
    api(Square.moshi.kotlinReflect)

    api(project(":http4k-api-jsonschema"))
    implementation("dev.forkhandles:values4k:_")
    implementation("dev.forkhandles:data4k:_")

    testImplementation(project(":http4k-core"))
    testImplementation(Square.moshi)

    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-jsonschema")))
}
