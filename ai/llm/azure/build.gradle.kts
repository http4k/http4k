import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI LLM Azure and Azure GitHubModels support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm-openai"))
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-ai-llm-core")))
}
