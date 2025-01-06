import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Ktor to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.ktor:ktor-server-core-jvm:_")

    testFixturesApi(testFixtures(project(":http4k-core")))
}
