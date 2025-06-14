import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI LLM OpenAI support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm-core"))
    api(project(":http4k-connect-ai-openai"))
    testApi(project(":http4k-connect-ai-openai-fake"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-ai-llm-core")))
}
