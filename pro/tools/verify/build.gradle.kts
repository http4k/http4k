plugins {
    id("org.http4k.pro")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("http4kVerify") {
            id = "org.http4k.verify"
            implementationClass = "org.http4k.verify.Http4kVerifyPlugin"
        }
    }
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-moshi"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
}

description = "http4k Supply Chain Verification Gradle Plugin"
