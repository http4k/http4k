description = "http4k Fanbelt backplane transport over Postgres LISTEN/NOTIFY"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt-exposed"))
    api(libs.postgresql)

    testImplementation(testFixtures(project(":http4k-fanbelt-exposed")))
}
