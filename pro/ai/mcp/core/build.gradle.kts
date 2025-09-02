import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k AI MCP Core"

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(platform(libs.forkhandles.bom))

    compileOnly(libs.kotlin.reflect)

    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    api(libs.kotshi.api)
    api(project(":http4k-api-jsonrpc"))
    api(project(":http4k-ai-core"))

    api(libs.values4k)
    api(libs.result4k)
    api(libs.time4k)

    ksp(libs.kotshi.compiler)

    testImplementation(project(":http4k-serverless-lambda"))
    testImplementation(project(":http4k-client-websocket"))

    testImplementation(libs.jsoup)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-security-core"))
    testImplementation(project(":http4k-testing-approval"))
}
