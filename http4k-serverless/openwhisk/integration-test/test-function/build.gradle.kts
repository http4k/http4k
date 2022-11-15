description = "Testing against a functions deployed to Apache OpenWhisk"

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":http4k-serverless-openwhisk"))
    api(project(path = ":http4k-core", configuration = "testArtifacts"))
    api(project(path = ":http4k-serverless-core", configuration = "testArtifacts"))
    compileOnly("com.google.code.gson:gson:_")
}
