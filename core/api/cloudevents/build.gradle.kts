description = "http4k CloudEvents support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api(libs.cloudevents.core)
    api(libs.jackson.datatype.guava) // for CVE workaround (guava)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
}

