import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
//    testFixturesApi(Libs.sqs)  FIXME why doesn't this work?
    testFixturesApi("software.amazon.awssdk:sqs:_")
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
