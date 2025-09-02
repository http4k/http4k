import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Pug4j templating support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.pug4j)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
