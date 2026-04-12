description = "Testing against a functions deployed to ACF"

plugins {
    alias(libs.plugins.shadow)
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-serverless-alibaba"))
    api(testFixtures(project(":http4k-core")))
}
