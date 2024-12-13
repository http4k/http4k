import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "DEPRECATED: Use http4k-ops-micrometer"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.micrometer:micrometer-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
