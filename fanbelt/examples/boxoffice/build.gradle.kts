plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-compression"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "boxoffice.MainKt"
}
