import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-amazon-core"))
    testFixturesApi("org.jetbrains.kotlin", "kotlin-reflect")
    testFixturesApi(project(":http4k-serverless-lambda"))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
    testFixturesApi(project(path = ":http4k-connect-amazon-s3"))
}
