import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Failsafe support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-failsafe"))
}

