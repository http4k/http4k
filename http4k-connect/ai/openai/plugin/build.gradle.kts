import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api( ":http4k-contract")
    api( ":http4k-format-jackson")
    api( ":http4k-security-oauth")
    api(project(":http4k-connect-storage-core"))

    compileOnly(platform("org.junit:junit-bom:_"))
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    compileOnly( ":http4k-testing-approval")
    compileOnly( ":http4k-testing-hamkrest")

    testApi(project(":http4k-connect-ai-openai-fake"))
    testApi("org.junit.jupiter:junit-jupiter-api")
    testApi( ":http4k-serverless-lambda")
    testApi( ":http4k-cloudnative")
    testApi( ":http4k-testing-approval")

    testApi("com.nimbusds:nimbus-jose-jwt:_")
}
