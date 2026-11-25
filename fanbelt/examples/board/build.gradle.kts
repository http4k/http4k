plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-fanbelt-exposed"))
    api(project(":http4k-fanbelt-postgres"))
    api(libs.hikaricp)
    api(project(":http4k-config"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-compression"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "board.MainKt"
}
