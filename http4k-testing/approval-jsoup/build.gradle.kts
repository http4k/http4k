description = "Approval Testing of HTML5 content with Jsoup"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-testing-approval"))
    api(Testing.junit.jupiter.api)
    implementation("org.jsoup:jsoup:_")

    testImplementation(project(":http4k-core"))
}
