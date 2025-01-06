import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Vertx to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.vertx:vertx-web:_")

    testFixturesApi(testFixtures(project(":http4k-core")))
}
