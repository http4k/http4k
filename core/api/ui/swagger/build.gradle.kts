import org.http4k.internal.ModuleLicense.Apache2

description = "Add a locally hosted Swagger UI to your server"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-api-openapi"))
    api(libs.swagger.ui)

    testImplementation(testFixtures(project(":http4k-core")))
}

