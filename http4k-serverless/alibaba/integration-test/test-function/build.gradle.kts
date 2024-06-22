description = "Testing against a functions deployed to ACF"

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":http4k-serverless-alibaba"))
    api(testFixtures(project(":http4k-core")))
}
