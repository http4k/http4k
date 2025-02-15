import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Bridge: Servlet to http4k"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation("javax.servlet:javax.servlet-api:_")
    implementation("jakarta.servlet:jakarta.servlet-api:_")
}
