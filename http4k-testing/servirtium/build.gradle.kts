import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "http4k Servirtium module"

dependencies {
    api(project(":http4k-core"))
    implementation(Testing.junit.jupiter.api)
    api(project(":http4k-cloudnative"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-client-apache"))
    testImplementation("org.apiguardian:apiguardian-api:_")

    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all")
        }
    }
}
