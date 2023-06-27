import KotlinX.reflect
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

description = "Http4k Jackson JSON support"

dependencies {
    api(project(":http4k-format-core"))

    implementation(project(":http4k-format-jackson"))
    implementation("dev.forkhandles:values4k:_")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(path = ":http4k-format-core", configuration = "testArtifacts"))
    testImplementation(project(":http4k-testing-approval"))
}
