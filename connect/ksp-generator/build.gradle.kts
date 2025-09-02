

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("com.google.devtools.ksp")
//    alias(libs.plugins.ksp) }

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
}

