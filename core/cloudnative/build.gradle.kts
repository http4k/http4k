import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: use http4k-cloud-core, http4k-cloud-k8s and http4k-config instead depending on the use case"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-cloud-k8s"))
}
