import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: Servlet to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation(libs.javax.servlet.api)
    implementation(libs.jakarta.servlet.api)

    testFixturesImplementation(libs.javax.servlet.api)
    testFixturesImplementation(libs.jakarta.servlet.api)
    testFixturesApi(libs.mock4k)
}
