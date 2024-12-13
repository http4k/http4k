import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Example enterprise client module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.enterprise")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
}
