import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: Servlet to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation("javax.servlet:javax.servlet-api:_")
    implementation("jakarta.servlet:jakarta.servlet-api:_")

    testFixturesImplementation("javax.servlet:javax.servlet-api:_")
    testFixturesImplementation("jakarta.servlet:jakarta.servlet-api:_")
}
