description = "http4k Fanbelt storage on SQLite"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt-exposed"))
    api(libs.sqlite.jdbc)
    implementation(libs.hikaricp)

    testImplementation(testFixtures(project(":http4k-fanbelt-exposed")))
}
