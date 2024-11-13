import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k GSON JSON support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.google.code.gson:gson:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-contract"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-contract")))
    testImplementation(testFixtures(project(":http4k-jsonrpc")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
