import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Jakarta to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("jakarta.inject:jakarta.inject-api:_")
    api("jakarta.ws.rs:jakarta.ws.rs-api:_")

    testFixturesApi(testFixtures(project(":http4k-core")))
    testApi("org.jboss.resteasy:resteasy-core:_")
}
