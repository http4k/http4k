

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api(libs.values4k)
    api(kotlin("script-runtime"))
}
