import org.http4k.internal.ModuleLicense.Http4kEnterprise

description = "Example enterprise client module"

val license by project.extra { Http4kEnterprise }

plugins {
    id("org.http4k.enterprise")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
}
