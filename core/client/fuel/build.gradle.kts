import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "HTTP Client built on top of fuel"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("com.github.kittinunf.fuel:fuel:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
