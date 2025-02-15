import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k HTTP Server built on top of Ratpack"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.ratpack:ratpack-core:_")

    // to overcome CVEs from outdated ratpack
    api(project(":http4k-format-jackson-yaml"))
    api(project(":http4k-server-netty"))
    api("com.google.guava:guava:_")

    testImplementation(testFixtures(project(":http4k-core")))
}
