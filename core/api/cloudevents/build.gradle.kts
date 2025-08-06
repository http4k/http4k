import org.http4k.internal.ModuleLicense.Apache2

description = "http4k CloudEvents support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api(libs.cloudevents.core)
    api(libs.cloudevents.json.jackson)
    api("com.fasterxml.jackson.datatype:jackson-datatype-guava:_") // for CVE workaround (guava)
    api(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
}

