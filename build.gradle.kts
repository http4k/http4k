plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.api-docs")
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}
