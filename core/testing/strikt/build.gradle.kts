import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "A set of Strikt matchers for common http4k types"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("io.strikt:strikt-core:_")
    testImplementation(project(":http4k-format-jackson"))
}
