description = "Testing against a functions deployed to SCF"

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":http4k-serverless-tencent"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
}
