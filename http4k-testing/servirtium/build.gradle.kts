import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "http4k incubator module"

dependencies {
    api(project(":http4k-core"))
    implementation(Testing.junit.api)
    api(project(":http4k-cloudnative"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-client-apache"))
    testImplementation("org.apiguardian:apiguardian-api:_")

    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all")
        }
    }
}
