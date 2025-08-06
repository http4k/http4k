import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-api-openapi"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-security-core"))
    api(libs.swagger.ui)
}
