import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k CloudEvents support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-cloudevents"))
}

