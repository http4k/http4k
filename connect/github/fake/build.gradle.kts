import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.project-metadata")
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
}

metadata {
    developers = mapOf(
        "David Denton" to "david@http4k.org",
        "Ivan Sanchez" to "ivan@http4k.org",
        "Albert Latacz" to "albert@http4k.org"
    )
}
