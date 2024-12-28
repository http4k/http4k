import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: use http4k-api-ui-redoc instead"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-api-openapi"))
    api("org.webjars:redoc:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

