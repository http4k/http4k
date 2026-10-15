description = "http4k Brotli and Zstandard content-encoding support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.zstd.jni)
    api(libs.brotli4j)
    runtimeOnly(libs.brotli4j.native.linux.aarch64)
    runtimeOnly(libs.brotli4j.native.linux.x64)
    runtimeOnly(libs.brotli4j.native.osx.aarch64)
    runtimeOnly(libs.brotli4j.native.osx.x64)
    runtimeOnly(libs.brotli4j.native.windows.x64)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-realtime-core"))
    testImplementation(project(":http4k-server-jetty"))
}
