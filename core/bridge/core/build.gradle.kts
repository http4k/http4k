description = "http4k Bridge: shared utilities for bridging frameworks to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
}
