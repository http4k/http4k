

description = "http4k Connect KSP code generator"

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-connect-core"))

    api(project(":http4k-format-moshi"))
    api(libs.kotlinpoet)
    api(libs.kotlinpoet.metadata)
    api(libs.kotlinpoet.ksp)
    api(libs.symbol.processing.api)

    ksp(libs.kotshi.compiler)

    testFixturesApi(libs.kotshi.api)
    testFixturesApi(project(":http4k-format-moshi"))
    testFixturesApi(libs.result4k)

    kspTest(project(":http4k-connect-ksp-generator"))
    kspTestFixtures(project(":http4k-connect-ksp-generator"))
    kspTestFixtures(libs.kotshi.compiler)
}

// the processor runs inside the Gradle daemon (JDK 21) so must not be compiled for a newer JVM
java {
    disableAutoTargetJvm()
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileKotlin") {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}
