import org.http4k.internal.ModuleLicense.Apache2

description = "http4k JSON Schema support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))

    implementation(project(":http4k-format-jackson"))
    implementation(project(":http4k-format-kondor-json"))
    implementation("dev.forkhandles:values4k:_")
    implementation("dev.forkhandles:data4k:_")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-format-moshi"))
    testImplementation(testFixtures(project(":http4k-format-core")))

    testFixturesApi(project(":http4k-testing-approval"))
}

