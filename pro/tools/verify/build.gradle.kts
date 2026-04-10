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
    api(libs.values4k)
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
}

tasks.named<Jar>("jar") {
    archiveVersion.set(project.properties["releaseVersion"]?.toString() ?: "LOCAL")
}

description = "http4k Supply Chain Verification Gradle Plugin"
