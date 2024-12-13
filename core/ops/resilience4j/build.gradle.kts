import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Resilience4j support"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-resilience4j"))
}

