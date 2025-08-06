plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.api-docs")
    id("nl.littlerobots.version-catalog-update") version "1.0.0"
    alias(libs.plugins.versions)
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}
