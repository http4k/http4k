import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-api-openapi"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-security-oauth"))
    api(project(":http4k-connect-storage-core"))

    compileOnly(platform("org.junit:junit-bom:_"))
    compileOnly("org.junit.jupiter:junit-jupiter-api:_")
    compileOnly(project(":http4k-testing-approval"))
    compileOnly(project(":http4k-testing-hamkrest"))

    testApi(project(":http4k-connect-ai-openai-fake"))
    testApi("org.junit.jupiter:junit-jupiter-api")
    testApi(project(":http4k-serverless-lambda"))
    testApi(project(":http4k-testing-approval"))

    testApi("com.nimbusds:nimbus-jose-jwt:_")
}
