import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Ops core library"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(platform("io.opentelemetry:opentelemetry-bom:_"))
    api("io.opentelemetry:opentelemetry-sdk-metrics")
    api("io.opentelemetry.semconv:opentelemetry-semconv:_")
}

