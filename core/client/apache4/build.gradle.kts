import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k HTTP Client built on top of apache-httpclient"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
