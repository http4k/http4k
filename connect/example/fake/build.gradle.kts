import org.http4k.internal.ModuleLicense.Http4kCommunity

val license by project.extra { Http4kCommunity }

plugins {
    // uncomment this for a real published module
    // id("org.http4k.community")
    id("org.http4k.conventions")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
// todo insert explicit dependencies here
}
