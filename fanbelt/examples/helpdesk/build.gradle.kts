plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-fanbelt-postgres"))
    api(project(":http4k-fanbelt-redis"))
    api(libs.hikaricp)
    api(project(":http4k-compression"))
    api(project(":http4k-connect-openfga"))
    api(project(":http4k-connect-openfga-fake"))
    runtimeOnly(libs.postgresql)
    api(project(":http4k-config"))
    api(project(":http4k-server-jetty"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "helpdesk.MainKt"
}
