import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP Client built on top of Helidon"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.webclient:helidon-webclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
