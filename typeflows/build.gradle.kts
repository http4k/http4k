plugins {
    id("org.http4k.community")
}
description = "http4k Typeflows project standards as code"

dependencies {
    api(libs.typeflows.github)
    api(libs.typeflows.github.marketplace)
}
