description = "Testing against a functions deployed to GCF"

plugins {
    id("org.http4k.conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":http4k-serverless-gcf"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
