import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Ops core library"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
}

