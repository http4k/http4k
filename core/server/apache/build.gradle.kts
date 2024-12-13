import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k HTTP Server built on top of Apache httpcore"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents.core5:httpcore5:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
