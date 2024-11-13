import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k multipart form support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-moshi"))
    testImplementation(testFixtures(project(":http4k-core")))
}
