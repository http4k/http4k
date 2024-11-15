import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-security-oauth"))
    implementation("org.bitbucket.b_c:jose4j:_")

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
