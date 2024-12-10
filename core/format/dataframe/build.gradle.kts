import org.http4k.internal.ModuleLicense.Apache2

description = "http4k KotlinX DataFrame support"

val license by project.extra { Apache2 }

plugins {
    id("org.jetbrains.kotlinx.dataframe")
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(KotlinX.dataframe)
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}

dataframes {
    sourceSet = "test"
    packageName = "org.http4k.format.dataframe"
}
