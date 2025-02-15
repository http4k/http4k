import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Security Digest support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
