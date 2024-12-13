import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Serverless core"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
}
