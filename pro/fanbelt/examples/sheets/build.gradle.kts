plugins {
    id("org.http4k.conventions")
}

dependencies {
    implementation(project(":http4k-fanbelt-sqlite"))
    implementation(project(":http4k-config"))
    implementation(project(":http4k-server-jetty"))
    implementation(project(":http4k-compression"))
    testImplementation(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "sheets.MainKt"
}
