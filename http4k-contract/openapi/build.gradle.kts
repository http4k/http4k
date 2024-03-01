import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

description = "http4k typesafe HTTP contracts and OpenApi support"

apply(plugin = "org.openapi.generator")

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-contract-jsonschema"))

    implementation("dev.forkhandles:values4k:_")
    implementation(project(":http4k-security-oauth"))
    implementation(project(":http4k-format-jackson"))
    implementation(project(":http4k-multipart"))

    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-format-argo"))
    testImplementation(project(":http4k-format-kondor-json"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-security-oauth")))
}

tasks.register<GenerateTask>("generateOpenApi3AutoClient") {
    generatorName = "kotlin"
    outputDir = "."
    validateSpec = false
    inputSpec =
        "$projectDir/src/test/resources/org/http4k/contract/openapi/v3/OpenApi3AutoTest.renders as expected.approved".toString()
    inputs.file(inputSpec)
}

tasks.register<GenerateTask>("generateOpenApi3Client") {
    generatorName = "kotlin"
    outputDir = "."
    validateSpec = false
    inputSpec =
        "$projectDir/src/test/resources/org/http4k/contract/openapi/v3/OpenApi3Test.renders as expected.approved".toString()
    inputs.file(inputSpec)
    mustRunAfter("generateOpenApi3AutoClient")
}

tasks.register<GenerateTask>("generateOpenApi2Client") {
    generatorName = "kotlin"
    outputDir = "."
    validateSpec = false
    inputSpec =
        "$projectDir/src/test/resources/org/http4k/contract/openapi/v2/OpenApi2Test.renders as expected.approved".toString()
    inputs.file(inputSpec)
    mustRunAfter("generateOpenApi3Client")
}

tasks {
    named("compileKotlin").get().dependsOn(
        named("generateOpenApi3AutoClient").get(),
        named("generateOpenApi3Client").get(),
        named("generateOpenApi2Client").get()
    )
    named("processResources").get().dependsOn(
        named("generateOpenApi3AutoClient").get(),
        named("generateOpenApi3Client").get(),
        named("generateOpenApi2Client").get()
    )
    named("processTestResources").get().dependsOn(
        named("generateOpenApi3AutoClient").get(),
        named("generateOpenApi3Client").get(),
        named("generateOpenApi2Client").get()
    )
}
