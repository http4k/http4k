import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Micronaut to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation("io.micronaut:micronaut-http:_")

    testFixturesApi(testFixtures(project(":http4k-core")))
}
