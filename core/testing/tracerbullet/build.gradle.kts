import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Tracer Bullet module"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-moshi"))
    api(libs.moshi.adapters)
    implementation(platform(libs.junit.bom))
    compileOnly("org.junit.jupiter:junit-jupiter-api")

    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-api-openapi")))
}
