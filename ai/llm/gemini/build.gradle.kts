

description = "http4k AI LLM Gemini support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm-openai"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-ai-llm-core")))
}
