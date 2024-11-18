import org.http4k.internal.ModuleLicense.Http4kEnterprise

description = "Test module"

val license by project.extra { Http4kEnterprise }

plugins {
    id("org.http4k.enterprise")
}

dependencies {
}
