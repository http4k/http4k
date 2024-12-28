import org.http4k.internal.ModuleLicense.Apache2

description = "Dependency-lite Server as a Function in pure Kotlin"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(Kotlin.stdlib)
    implementation("javax.servlet:javax.servlet-api:_")
    implementation("jakarta.servlet:jakarta.servlet-api:_")
    implementation("dev.forkhandles:result4k:_")
    implementation("dev.forkhandles:values4k:_")

    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testFixturesImplementation("org.junit.platform:junit-platform-launcher:_")
    testFixturesImplementation(platform("io.opentelemetry:opentelemetry-bom:_"))
    testFixturesImplementation("io.opentelemetry:opentelemetry-sdk")
    testFixturesImplementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testFixturesImplementation("javax.servlet:javax.servlet-api:_")
    testFixturesImplementation("jakarta.servlet:jakarta.servlet-api:_")
    testFixturesImplementation("dev.forkhandles:result4k:_")
    testFixturesImplementation("dev.forkhandles:values4k:_")
    testFixturesApi(project(":http4k-client-apache4"))
    testFixturesApi(project(":http4k-testing-approval"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-server-apache"))
    testFixturesApi("dev.forkhandles:mock4k:_")
    testFixturesApi("org.webjars:swagger-ui:_")
}
