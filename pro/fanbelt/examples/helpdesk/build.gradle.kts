plugins {
    id("org.http4k.conventions")
}

dependencies {
    implementation(project(":http4k-fanbelt-postgres"))
    implementation(project(":http4k-fanbelt-redis"))
    implementation(libs.hikaricp)
    implementation(project(":http4k-compression"))
    implementation(project(":http4k-connect-openfga"))
    implementation(project(":http4k-connect-openfga-fake"))
    runtimeOnly(libs.postgresql)
    implementation(project(":http4k-config"))
    implementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "helpdesk.MainKt"
}
