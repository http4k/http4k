description = "Http4k Serverless support for Apache OpenWhisk"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-gson"))
}
