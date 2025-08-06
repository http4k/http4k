import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Server built on top of Apache httpcore"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.apache.httpcore)
    api("commons-codec:commons-codec:_") // override version provided by httpcore (Cxeb68d52e-5509)
    testImplementation(testFixtures(project(":http4k-core")))
}
