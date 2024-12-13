import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Thymeleaf templating support"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api("org.thymeleaf:thymeleaf:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
