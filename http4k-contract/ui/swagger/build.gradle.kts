import org.http4k.internal.ModuleLicense.Apache2

description = "Add a locally hosted Swagger UI to your server"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-contract"))
    api("org.webjars:swagger-ui:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

