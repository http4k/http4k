import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Spring to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-server-servlet"))
    api("javax.servlet:javax.servlet-api:_")

    implementation("org.springframework:spring-web:_")
    implementation("org.springframework:spring-context:_")

    testFixturesApi(testFixtures(project(":http4k-server-servlet")))
}
