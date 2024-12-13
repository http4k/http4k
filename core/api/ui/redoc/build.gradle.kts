import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "Add a locally hosted Redoc UI to your server"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-contract-ui-redoc"))
}

