import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Example Pro fake module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
}
