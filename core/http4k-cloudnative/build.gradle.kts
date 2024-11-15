import org.http4k.internal.ModuleLicense.Apache2

description = "Machinery for running Http4k apps in cloud-native environments"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-config"))
    implementation(project(":http4k-format-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
