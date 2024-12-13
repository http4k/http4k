import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Security Core support"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
