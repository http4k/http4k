import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k extensions for testing with Playwright"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("com.microsoft.playwright:playwright:_")
    api("org.junit.jupiter:junit-jupiter-api:_")

    testImplementation(testFixtures(project(":http4k-core")))

}
