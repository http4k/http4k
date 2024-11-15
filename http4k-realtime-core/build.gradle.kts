import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k WebSocket core"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi("com.launchdarkly:okhttp-eventsource:_")
}
