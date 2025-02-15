import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k support for GraphQL"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-graphql"))
}

