description = "Dependency-lite Server as a Function in pure Kotlin"

dependencies {
    api(Kotlin.stdlib)
    api(platform("dev.forkhandles:forkhandles-bom:_"))

    implementation("javax.servlet:javax.servlet-api:_")
    implementation("jakarta.servlet:jakarta.servlet-api:_")
    implementation("dev.forkhandles:result4k")
    implementation("dev.forkhandles:values4k")

    testImplementation(Testing.junit.jupiter.params)

    testFixturesImplementation("javax.servlet:javax.servlet-api:_")
    testFixturesImplementation("jakarta.servlet:jakarta.servlet-api:_")
    testFixturesImplementation("dev.forkhandles:result4k")
    testFixturesImplementation("dev.forkhandles:values4k")
    testFixturesApi(project(":http4k-client-apache"))
    testFixturesApi(project(":http4k-testing-approval"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-server-apache"))
    testFixturesApi("dev.forkhandles:mock4k")
    testFixturesApi("org.webjars:swagger-ui:_")
}
