import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP client built on top of Netty"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.netty:netty-codec-http2:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
