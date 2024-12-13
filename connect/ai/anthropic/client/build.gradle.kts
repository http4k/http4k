import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-ai-core"))

    api("se.ansman.kotshi:api:_")

    testApi(project(":http4k-format-moshi"))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
}
