import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-connect-amazon-s3"))
    api(project(":http4k-format-moshi"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-s3")))
    testFixturesApi(project(":http4k-connect-amazon-s3-fake"))
}
