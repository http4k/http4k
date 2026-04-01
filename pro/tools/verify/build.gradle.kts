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
}

description = "http4k Supply Chain Verification Gradle Plugin"
