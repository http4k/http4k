plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(project(":http4k-fanbelt-postgres"))
    api(project(":http4k-fanbelt-redis"))
    api(libs.hikaricp)
    runtimeOnly(libs.postgresql)
    api(project(":http4k-config"))
    api(project(":http4k-server-jetty"))
    api(project(":http4k-compression"))
    testFixturesApi(project(":http4k-fanbelt-testing"))
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "features.MainKt"
}

tasks.register<JavaExec>("runDistributed") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "features.DistributedKt"
}

tasks.register<JavaExec>("runCluster") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "features.ClusterKt"
}
