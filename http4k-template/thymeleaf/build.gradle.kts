import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Thymeleaf templating support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-template-core"))
    api("org.thymeleaf:thymeleaf:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
