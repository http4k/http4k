import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Cloud core"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-config"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
