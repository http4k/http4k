plugins {
    id("org.http4k.conventions")
}

dependencies {
    implementation(project(":http4k-fanbelt-examples-sheets"))
    implementation(project(":http4k-fanbelt-examples-boxoffice"))
    implementation(project(":http4k-fanbelt-postgres"))
    implementation(project(":http4k-fanbelt-redis"))
    implementation(libs.hikaricp)
    implementation(project(":http4k-compression"))
    runtimeOnly(libs.postgresql)
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.http4k.fanbelt.load.MainKt"
    workingDir = layout.buildDirectory.get().asFile
    args = (project.findProperty("scenarios") as String?)?.split(",").orEmpty()
}
