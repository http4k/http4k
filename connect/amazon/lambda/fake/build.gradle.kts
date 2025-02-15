import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    testFixturesApi("com.amazonaws:aws-lambda-java-events:_")
    testFixturesApi(project(":http4k-format-moshi"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
