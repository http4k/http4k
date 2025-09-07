plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.api-docs")
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.typeflows)
    id("org.http4k.conventions")
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}

dependencies {
    typeflowsApi(libs.typeflows.github)
    typeflowsApi(libs.typeflows.github.marketplace)
}

typeflows {
    typeflowsClass = "Http4kTypeflows"
}
