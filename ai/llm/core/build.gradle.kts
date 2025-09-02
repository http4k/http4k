

description = "http4k AI universal LLM interfaces and types"

plugins {
    id("org.http4k.community")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-core"))
    implementation(project(":http4k-connect-storage-core"))

    api(libs.kotshi.api)
    ksp(libs.kotshi.compiler)

    testFixturesApi(project(":http4k-api-jsonrpc"))
    testFixturesApi(project(":http4k-connect-storage-core"))
}
