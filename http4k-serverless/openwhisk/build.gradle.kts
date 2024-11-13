description = "Http4k Serverless support for Apache OpenWhisk"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-gson"))
}
