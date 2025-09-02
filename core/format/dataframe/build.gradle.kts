

description = "http4k KotlinX DataFrame support"

plugins {
    kotlin("jvm")
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(libs.kotlinx.dataframe)
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(libs.kotlin.reflect)
}
