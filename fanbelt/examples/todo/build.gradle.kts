plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(project(":http4k-config"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-compression"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "todo.MainKt"
}
