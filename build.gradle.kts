plugins {
    id("org.http4k.metadata")
    id("org.http4k.nexus")
}

http4kMetadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}
