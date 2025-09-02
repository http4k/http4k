import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-amazon-s3"))
    api(project(":http4k-connect-ai-openai"))
    api(project(":http4k-connect-ai-ollama"))
    api(project(":http4k-connect-ai-lmstudio"))
    api(libs.langchain4j.core)
    api(libs.langchain4j.http.client)

    testFixturesApi(libs.langchain4j.open.ai)

    testFixturesApi(libs.langchain4j)
    testFixturesApi(project(":http4k-connect-ai-openai-fake"))
    testFixturesApi(project(":http4k-connect-ai-ollama-fake"))
    testFixturesApi(project(":http4k-connect-ai-lmstudio-fake"))
    testFixturesApi(project(":http4k-connect-amazon-s3-fake"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
