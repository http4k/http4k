import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: use http4k-api-cloudevents instead"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api("io.cloudevents:cloudevents-core:_")
    api("io.cloudevents:cloudevents-json-jackson:_")
    api("com.fasterxml.jackson.datatype:jackson-datatype-guava") // for CVE workaround (guava)
    api(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
}
