import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k K8S integration tooling"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-platform-core"))
    api(project(":http4k-config"))
    implementation(project(":http4k-format-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-config"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
