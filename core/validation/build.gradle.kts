description = "Composable, message-carrying request validation for http4k, extending values4k"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(libs.values4k)

    compileOnly(libs.result4k)

    testImplementation(libs.result4k)
}
