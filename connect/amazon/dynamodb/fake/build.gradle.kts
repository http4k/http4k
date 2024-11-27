import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api("dev.forkhandles:parser4k")
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
    testFixturesApi(project(path = ":http4k-connect-amazon-s3"))
}
