import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Jakarta to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(libs.jakarta.inject.api)
    implementation(libs.jakarta.ws.rs.api)

    testFixturesApi(testFixtures(project(":http4k-core")))
    testApi(libs.resteasy.core)
}
