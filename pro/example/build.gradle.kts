import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Test Pro module"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
}
