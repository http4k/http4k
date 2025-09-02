import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-ai-core"))
    api(libs.kotshi.api)

    testApi(project(":http4k-format-moshi"))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
}
