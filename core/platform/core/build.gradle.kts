import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Cloud core"

val license by project.extra { Apache2 }

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
