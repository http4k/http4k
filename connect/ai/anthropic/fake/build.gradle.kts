import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(libs.loremipsum)
}
