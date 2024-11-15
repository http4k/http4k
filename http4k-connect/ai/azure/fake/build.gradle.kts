import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-connect-ai-azure"))
    api( ":http4k-template-pebble")
    api( ":http4k-contract-ui-swagger")
    implementation("de.sven-jacobs:loremipsum:_")
}
