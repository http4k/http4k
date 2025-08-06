import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Spring to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-bridge-servlet"))
    api(libs.jakarta.servlet.api)

    implementation(libs.spring.webmvc)
    implementation(libs.spring.context)
    
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(libs.spring.test)
    testFixturesApi(testFixtures(project(":http4k-bridge-servlet")))
    testRuntimeOnly(libs.jackson.databind)
}
