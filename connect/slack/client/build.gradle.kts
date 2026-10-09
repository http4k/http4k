plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api(project(":http4k-security-oauth"))
    api(libs.kotshi.api)
}
