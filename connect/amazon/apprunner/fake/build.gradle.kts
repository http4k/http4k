import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    testFixturesApi(project(":http4k-format-moshi"))
    testFixturesApi(project(path = ":http4k-connect-amazon-iamidentitycenter"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
