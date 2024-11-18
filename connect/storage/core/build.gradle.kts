import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api("dev.forkhandles:values4k")
    api(kotlin("script-runtime"))
}
