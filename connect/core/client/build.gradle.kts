import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-platform-core"))
    api("dev.forkhandles:values4k")
    compileOnly(project(":http4k-format-moshi"))
    testCompileOnly(project(":http4k-format-moshi"))
}
