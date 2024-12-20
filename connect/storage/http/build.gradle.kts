import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-api-openapi"))
    api(project(":http4k-format-jackson"))
    api("org.webjars:swagger-ui:_")
}
