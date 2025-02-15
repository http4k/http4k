import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Pebble templating support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api("io.pebbletemplates:pebble:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
