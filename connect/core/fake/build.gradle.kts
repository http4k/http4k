import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-core"))
    api(project(":http4k-connect-storage-core"))
    api(project(":http4k-testing-chaos"))
    api(project(":http4k-format-moshi"))

    testFixturesApi(testFixtures(project(":http4k-core")))
}
