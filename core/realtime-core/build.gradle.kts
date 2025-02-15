import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Realtime core"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testApi(testFixtures(project(":http4k-core")))

    testFixturesApi("io.mockk:mockk:_")
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-web-datastar"))
    testFixturesApi("com.launchdarkly:okhttp-eventsource:_")
}
