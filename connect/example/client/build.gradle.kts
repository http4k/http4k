import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    // uncomment this for a real published module
    // id("org.http4k.community")
    id("org.http4k.conventions")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    // todo insert explicit dependencies here
}
