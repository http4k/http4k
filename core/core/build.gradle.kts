import org.http4k.internal.ModuleLicense.Apache2

description = "Dependency-lite Server as a Function in pure Kotlin"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(Kotlin.stdlib)
    implementation(libs.result4k)
    implementation(libs.values4k)

    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testFixturesImplementation(platform("io.opentelemetry:opentelemetry-bom:_"))
    testFixturesImplementation("io.opentelemetry:opentelemetry-sdk")
    testFixturesImplementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testFixturesImplementation(libs.result4k)
    testFixturesImplementation(libs.values4k)
    testFixturesApi(project(":http4k-client-apache4"))
    testFixturesApi(project(":http4k-testing-approval"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-server-apache"))
    testFixturesApi(libs.mock4k)
    testFixturesApi("org.webjars:swagger-ui:_")
}
