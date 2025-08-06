import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Ratpack to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.ratpack.core)

    // to overcome CVEs from outdated ratpack
    api(project(":http4k-format-jackson-yaml"))
    api(project(":http4k-server-netty"))
    api(libs.guava)

    testImplementation(testFixtures(project(":http4k-core")))
}
