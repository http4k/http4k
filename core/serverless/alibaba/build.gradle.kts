import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Serverless support for Alibaba Function Compute"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-bridge-servlet"))
    api(project(":http4k-format-moshi"))
    api(libs.aliyun.fc.java.core)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-bridge-servlet")))
}
