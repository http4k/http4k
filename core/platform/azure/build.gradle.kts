import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Azure integration tooling"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("com.azure:azure-core:_")
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-platform-core"))
    testFixturesImplementation(project(":http4k-config"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

