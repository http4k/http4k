description = "Testing against a functions deployed to GCF"

plugins {
    id("org.http4k.conventions")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":http4k-serverless-gcf"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
