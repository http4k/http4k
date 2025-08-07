description = "Testing against a functions deployed to SCF"

plugins {
    id("org.http4k.conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":http4k-serverless-tencent"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
