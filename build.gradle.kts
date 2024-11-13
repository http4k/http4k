plugins {
    id("org.http4k.nexus")
    id("org.http4k.metadata")
}

http4kMetadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org"
    )
}
