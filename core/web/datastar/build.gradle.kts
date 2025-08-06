import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Datastar support utilities"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(project(":http4k-format-core"))
    implementation(project(":http4k-template-core"))

    api(libs.values4k)

    testImplementation(testFixtures(project(":http4k-core")))

    // TEMP
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-web-datastar"))
    testImplementation(project(":http4k-template-handlebars"))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-format-moshi"))

}
