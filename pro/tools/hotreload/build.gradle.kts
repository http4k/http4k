import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Tools: Hot Reload support"

plugins {
    id("org.http4k.pro")
}

dependencies {
    implementation(project(":http4k-realtime-core"))

    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-server-helidon"))
}
