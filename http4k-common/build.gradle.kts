description = "Dependency-lite Server as a Function in pure Kotlin"

dependencies {
    api(Kotlin.stdlib)

    testImplementation(Testing.junit.jupiter.params)

    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-testing-approval"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi("org.webjars:swagger-ui:_")
}
