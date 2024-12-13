import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "DEPRECATED: use http4k-api-jsonrpc instead"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
