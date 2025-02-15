import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Add a locally hosted Swagger UI to your server"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-contract-ui-swagger"))
}

