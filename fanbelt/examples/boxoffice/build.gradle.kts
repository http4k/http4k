plugins {
    id("org.http4k.conventions")
}

dependencies {
    implementation(project(":http4k-fanbelt"))
    implementation(project(":http4k-server-jetty"))
    implementation(project(":http4k-compression"))
    testImplementation(project(":http4k-fanbelt-testing"))
    testImplementation(project(":http4k-testing-approval"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "boxoffice.MainKt"
}
