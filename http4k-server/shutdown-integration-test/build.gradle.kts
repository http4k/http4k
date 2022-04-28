description = "Integration tests for all servers"

plugins {
    application
}

application {
    mainClass.set("org.http4k.testing.TestServerKt")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(project(":http4k-server-apache"))
    implementation(project(":http4k-server-undertow"))
    implementation(project(":http4k-server-ratpack"))
    implementation(project(":http4k-cloudnative"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-realtime-core", configuration ="testArtifacts"))
    testImplementation("com.github.docker-java:docker-java-core:_")
    testImplementation("com.github.docker-java:docker-java-transport-httpclient5:_")
}

tasks.test {
    filter {
        exclude("integration/**")
    }
}

tasks.register<Test>("integrationTests") {
    description = "Runs docker-based server shutdown tests."
    group = "verification"

    useJUnitPlatform()

    filter {
        include("integration/**")
    }
}
