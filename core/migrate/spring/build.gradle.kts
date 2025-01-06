import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Migrate: from Spring to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-server-servlet"))

    implementation("org.springframework:spring-web:6.2.1")
    implementation("org.springframework:spring-context:6.2.1")

    implementation("javax.servlet:javax.servlet-api:_")
    implementation("jakarta.servlet:jakarta.servlet-api:_")

    testFixturesApi(testFixtures(project(":http4k-server-servlet")))
}
