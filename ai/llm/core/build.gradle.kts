import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI universal LLM interfaces and types"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-core"))
    implementation(project(":http4k-connect-storage-core"))

    api("se.ansman.kotshi:api:_")
    ksp("se.ansman.kotshi:compiler:_")

    testFixturesApi(project(":http4k-api-jsonrpc"))
    testFixturesApi(project(":http4k-connect-storage-core"))
}
