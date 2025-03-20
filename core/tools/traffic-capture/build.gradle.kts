import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Tools for HTTP Traffic Capture/Playback"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    compileOnly(project(":http4k-connect-storage-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-connect-storage-core"))
}
