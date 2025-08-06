description = "Testing against a functions deployed to SCF"

plugins {
    id("org.http4k.conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    api(project(":http4k-serverless-tencent"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
