import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api("io.lettuce:lettuce-core:_")
}
