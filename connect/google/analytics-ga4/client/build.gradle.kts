import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-google-analytics-core"))
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    api(libs.kotshi.api)

    api(project(":http4k-format-core"))
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org",
        "Albert Latacz" to "albert@http4k.org"
    )
}
