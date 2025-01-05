import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Servlet support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    api("javax.servlet:javax.servlet-api:_")
    api("jakarta.servlet:jakarta.servlet-api:_")
}
