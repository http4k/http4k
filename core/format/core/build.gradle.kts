import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Format supporting code"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation("dev.forkhandles:values4k:_")

    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation("dev.forkhandles:values4k:_")
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(testFixtures(project(":http4k-contract")))
    testFixturesImplementation(testFixtures(project(":http4k-jsonrpc")))
}
