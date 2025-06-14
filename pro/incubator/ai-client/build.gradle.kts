import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k chat client"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.conventions")
//    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-llm-core"))
    api(project(":http4k-ai-mcp-sdk"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-web-datastar"))
    api(project(":http4k-template-handlebars"))
    api(project(":http4k-ai-llm-anthropic"))
    api("org.jetbrains.kotlin:kotlin-reflect:_")

    api("se.ansman.kotshi:api:_")

    ksp("se.ansman.kotshi:compiler:_")
}
