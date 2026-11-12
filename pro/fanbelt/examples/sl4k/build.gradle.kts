plugins {
    id("org.http4k.conventions")
    application
}

dependencies {
    implementation(project(":http4k-fanbelt-core"))
    implementation(project(":http4k-fanbelt-redis"))
    implementation(project(":http4k-config"))
    implementation(project(":http4k-connect-core"))
    implementation(project(":http4k-server-jetty"))
    implementation(libs.jsoup)
    implementation(project(":http4k-compression"))
    testImplementation(project(":http4k-fanbelt-testing"))
}

application {
    mainClass = "sl4k.MainKt"
    applicationName = "sl4k"
}

tasks.named<JavaExec>("run") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "sl4k.LocalMainKt"
}
