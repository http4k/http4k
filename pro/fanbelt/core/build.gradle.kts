description = "http4k Fanbelt: live, multi-user web apps over Datastar"

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(platform(libs.forkhandles.bom))
    api(libs.values4k)
    api(libs.tx4k)
    api(project(":http4k-core"))
    api(project(":http4k-config"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-template-handlebars"))
    api(project(":http4k-web-datastar"))
    api(libs.kotshi.api)

    ksp(libs.kotshi.compiler)

    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.time4k)

    testImplementation(libs.time4k)
    testImplementation(project(":http4k-server-jetty"))
}

tasks.test {
    jvmArgs("-Djdk.virtualThreadScheduler.parallelism=1", "-Djdk.virtualThreadScheduler.maxPoolSize=1")
}
