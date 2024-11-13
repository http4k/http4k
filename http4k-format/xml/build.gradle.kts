import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k XML support using GSON as an underlying engine"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-format-gson"))
    api("org.json:json:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
