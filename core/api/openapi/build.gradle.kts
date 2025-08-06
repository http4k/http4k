import org.http4k.internal.ModuleLicense.Apache2
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

description = "http4k typesafe HTTP contracts and OpenApi support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    if (false) {
        id("org.openapi.generator")
    }
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:_")
    }
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-api-jsonschema"))
    api(project(":http4k-security-core"))

    implementation(libs.values4k)
    implementation(project(":http4k-security-oauth"))
    implementation(project(":http4k-format-jackson"))
    implementation(project(":http4k-multipart"))

    testImplementation(libs.values4k)
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-format-argo"))
    testImplementation(project(":http4k-format-kondor-json"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-security-oauth")))
}

tasks {
    if (false) {
        register<GenerateTask>("generateOpenApi3AutoClient") {
            generatorName = "kotlin"
            outputDir = "./build"
            validateSpec = false
            inputSpec =
                "$projectDir/src/test/resources/org/http4k/contract/openapi/v3/OpenApi3AutoTest.renders as expected.approved".toString()
            inputs.file(inputSpec)
        }

        register<GenerateTask>("generateOpenApi3Client") {
            generatorName = "kotlin"
            outputDir = "./build"
            validateSpec = false
            inputSpec =
                "$projectDir/src/test/resources/org/http4k/contract/openapi/v3/OpenApi3Test.renders as expected.approved".toString()
            inputs.file(inputSpec)
            mustRunAfter("generateOpenApi3AutoClient")
        }

        register<GenerateTask>("generateOpenApi2Client") {
            generatorName = "kotlin"
            outputDir = "./build"
            validateSpec = false
            inputSpec =
                "$projectDir/src/test/resources/org/http4k/contract/openapi/v2/OpenApi2Test.renders as expected.approved".toString()
            inputs.file(inputSpec)
            mustRunAfter("generateOpenApi3Client")
        }

        listOf(
            "compileKotlin",
            "compileTestKotlin",
            "processResources",
            "processTestResources",
            "dokkaHtmlPartial",
        ).forEach {
            named(it).get().dependsOn(
                named("generateOpenApi3AutoClient").get(),
                named("generateOpenApi3Client").get(),
                named("generateOpenApi2Client").get()
            )
        }
    }
}

