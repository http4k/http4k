import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k MCP Desktop support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("org.http4k.connect.module")
    id("org.graalvm.buildtools.native") version "0.9.28"
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            imageName.set("mcp-desktop")
            mainClass.set("org.http4k.mcp.McpDesktop")
            useFatJar.set(true)
            sharedLibrary.set(false)
        }
    }
}

dependencies {
    api("dev.forkhandles:bunting4k")
    api(project(":http4k-mcp-sdk"))
    api(project(":http4k-security-oauth"))
    api(project(":http4k-client-websocket"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-helidon"))
}
