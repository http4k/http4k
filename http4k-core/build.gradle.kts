description = "Dependency-lite Server as a Function in pure Kotlin"

dependencies {
    api(Kotlin.stdlib.jdk8)
    implementation("javax.servlet:javax.servlet-api:_")
    implementation("dev.forkhandles:result4k:_")
    implementation("dev.forkhandles:values4k:_")

    testApi(project(":http4k-client-apache"))
    testApi(project(":http4k-client-websocket"))
    testApi(project(":http4k-testing-approval"))
    testApi(project(":http4k-testing-hamkrest"))
    testApi(project(":http4k-server-apache"))
    testApi("org.webjars:swagger-ui:3.44.0") // leave hardcoded - tests
    testApi("com.launchdarkly:okhttp-eventsource:_")
    testApi("org.slf4j:slf4j-nop:_")

    testImplementation(Testing.junit.jupiter.params)
}
