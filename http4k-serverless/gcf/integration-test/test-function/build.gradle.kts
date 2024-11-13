description = "Testing against a functions deployed to GCF"

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    api(project(":http4k-serverless-gcf"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
