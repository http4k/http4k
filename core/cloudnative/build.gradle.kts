import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "DEPRECATED: use http4k-platform-core, http4k-platform-k8s and http4k-config instead depending on the use case"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-platform-k8s"))
}
