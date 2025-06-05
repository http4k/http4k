import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI LLM Anthropic support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-ai-llm"))
    api(project(":http4k-connect-ai-anthropic"))
    testApi(project(":http4k-connect-ai-anthropic-fake"))
}
