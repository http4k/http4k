import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP Client built on top of okhttp"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(Square.okHttp3)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-server-undertow"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}
