import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k HTTP Client built on top of apache-httpclient"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
