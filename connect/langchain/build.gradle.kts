import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Deprecated. Use : http4k-connect-ai-langchain"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-ai-langchain"))
}
