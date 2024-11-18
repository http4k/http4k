import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-amazon-core"))
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api("se.ansman.kotshi:api:_")

    testFixturesApi("io.mockk:mockk:_")
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
