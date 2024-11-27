import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k GCP integration tooling"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("com.google.http-client:google-http-client:_")

    testImplementation(project(":http4k-testing-hamkrest"))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-cloudnative"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

