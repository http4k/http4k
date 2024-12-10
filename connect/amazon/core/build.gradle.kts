import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-aws"))

    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api("se.ansman.kotshi:api:_")
    api("com.github.javadev:underscore:_")
    api(project(":http4k-format-core"))

    testFixturesApi(project(":http4k-testing-chaos"))
    testFixturesApi(testFixtures(project(":http4k-core")))
}
