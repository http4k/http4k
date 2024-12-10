description = "Integration tests for all servers"

plugins {
    id("org.http4k.conventions")
    application
}

application {
    mainClass.set("org.http4k.testing.TestServerKt")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-server-apache"))
    api(project(":http4k-server-apache4"))
    api(project(":http4k-server-undertow"))
    api(project(":http4k-server-ratpack"))
    api(project(":http4k-server-helidon"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-server-netty"))
    api(project(":http4k-server-ktornetty"))
    api(project(":http4k-server-ktorcio"))
    api(project(":http4k-platform-core"))
    api(project(":http4k-config"))
    api(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
    testImplementation(project(":http4k-client-jetty"))
    testImplementation("com.github.docker-java:docker-java-core:_")
    testImplementation("com.github.docker-java:docker-java-transport-httpclient5:_")
}

tasks.test {
    filter {
        exclude("integration/**")
    }
}

tasks.register<Test>("integrationTests") {
    dependsOn("distZip")

    description = "Runs docker-based server shutdown tests."
    group = "verification"

    useJUnitPlatform()

    filter {
        include("integration/**")
    }
}
