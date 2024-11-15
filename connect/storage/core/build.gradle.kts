import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
}

dependencies {
    implementation(project(":http4k-format-moshi"))
    implementation("dev.forkhandles:values4k")
    implementation(kotlin("script-runtime"))
}
