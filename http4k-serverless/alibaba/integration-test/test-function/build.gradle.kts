description = "Testing against a functions deployed to ACF"

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":http4k-serverless-alibaba"))
    api(project(path = ":http4k-core", configuration = "testArtifacts"))
}
