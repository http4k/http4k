import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.http4k.conventions")
}

configurations {
    named<Configuration>("runtimeClasspath") {
        isTransitive = true
    }
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-serverless-lambda"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    api("com.amazonaws:aws-lambda-java-events:_")
    api("joda-time:joda-time:_")
}

tasks {
    register<Zip>("buildZip") {
        from(named<KotlinCompile>("compileKotlin"))
        from(named<ProcessResources>("processResources"))
        into("lib") {
            from(configurations.named<Configuration>("runtimeClasspath"))
        }
    }
}
