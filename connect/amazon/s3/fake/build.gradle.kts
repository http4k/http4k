import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-template-pebble"))
    testFixturesApi(platform(libs.awssdk.bom))
    testFixturesApi(libs.awssdk.s3)
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
