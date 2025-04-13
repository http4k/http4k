import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k MCP Core"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    api("se.ansman.kotshi:api:_")
    api(project(":http4k-api-jsonrpc"))
    api(project(":http4k-connect-ai-core"))

    api("dev.forkhandles:values4k")
    api("dev.forkhandles:result4k")
    api("dev.forkhandles:time4k")

    ksp("se.ansman.kotshi:compiler:_")

    testImplementation(project(":http4k-serverless-lambda"))
    testImplementation(project(":http4k-client-websocket"))

    testImplementation("org.jsoup:jsoup:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-security-core"))
    testImplementation(project(":http4k-testing-approval"))
}
