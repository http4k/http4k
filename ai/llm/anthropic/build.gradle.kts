

description = "http4k AI LLM Anthropic support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm-core"))
    api(project(":http4k-connect-ai-anthropic"))
    testApi(project(":http4k-connect-ai-anthropic-fake"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-ai-llm-core")))
}
