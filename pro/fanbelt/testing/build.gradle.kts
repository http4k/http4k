description = "http4k Fanbelt testing: drive an app through an emulated or real browser"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt-core"))
    api(libs.time4k)
    api(project(":http4k-testing-webdriver-datastar"))
    api(project(":http4k-testing-playwright"))
    api(project(":http4k-testing-tracerbullet"))
    api(libs.junit.jupiter.api)

    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-testing-approval"))
}
