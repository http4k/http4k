import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k chat client"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-llm"))
    api(project(":http4k-mcp-sdk"))
    api(project(":http4k-server-jetty"))
    api("se.ansman.kotshi:api:_")

    ksp("se.ansman.kotshi:compiler:_")
}
