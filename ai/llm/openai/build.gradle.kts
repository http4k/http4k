

description = "http4k AI LLM OpenAI support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm-core"))
    api(project(":http4k-connect-ai-openai"))
    testImplementation(project(":http4k-connect-ai-openai-fake"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-ai-llm-core")))
}
