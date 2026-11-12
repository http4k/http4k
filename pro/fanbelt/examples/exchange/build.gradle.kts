plugins {
    id("org.http4k.conventions")
}

dependencies {
    implementation(project(":http4k-fanbelt-sqlite"))
    implementation(project(":http4k-fanbelt-redis"))
    implementation(project(":http4k-fanbelt-postgres"))
    implementation(libs.hikaricp)
    implementation(project(":http4k-config"))
    implementation(project(":http4k-server-jetty"))
    implementation(project(":http4k-server-helidon"))
    implementation(project(":http4k-server-undertow"))
    implementation(project(":http4k-compression"))
    testImplementation(project(":http4k-fanbelt-testing"))
}

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "exchange.MainKt"
}

val swarm by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations["swarmImplementation"].extendsFrom(configurations.implementation.get())

dependencies {
    "swarmImplementation"(project(":http4k-testing-playwright"))
}

tasks.register<JavaExec>("swarm") {
    group = "application"
    classpath = swarm.runtimeClasspath
    mainClass = "exchange.SwarmKt"
}

tasks.register<JavaExec>("httpSwarm") {
    group = "application"
    classpath = swarm.runtimeClasspath
    mainClass = "exchange.HttpSwarmKt"
}
