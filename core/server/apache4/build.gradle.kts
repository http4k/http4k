import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k HTTP Server built on top of Apache httpcore"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpcore:_")
    api("commons-codec:commons-codec:_") // override version provided by httpcore (Cxeb68d52e-5509)
    testImplementation(testFixtures(project(":http4k-core")))
}
