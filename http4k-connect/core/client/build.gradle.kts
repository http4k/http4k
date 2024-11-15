import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
}

dependencies {
    api("dev.forkhandles:values4k")
    compileOnly( ":http4k-format-moshi")
    testCompileOnly( ":http4k-format-moshi")
}
