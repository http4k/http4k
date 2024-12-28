import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: Use http4k-platform-aws"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation("software.amazon.awssdk:http-client-spi:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-client-okhttp"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-platform-core"))
    testFixturesImplementation(project(":http4k-config"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}
