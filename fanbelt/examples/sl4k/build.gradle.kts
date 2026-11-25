plugins {
    id("org.http4k.conventions")
    application
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(project(":http4k-fanbelt-redis"))
    api(project(":http4k-config"))
    api(project(":http4k-connect-core"))
    api(project(":http4k-server-jetty"))
    api(libs.jsoup)
    api(project(":http4k-compression"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
    testImplementation(testFixtures(project(":http4k-core")))
}

application {
    mainClass = "sl4k.MainKt"
    applicationName = "sl4k"
}

tasks.named<JavaExec>("run") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "sl4k.LocalMainKt"
}
