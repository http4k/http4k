import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "Deprecated. Use : :http4k-connect-ai-openai-plugin"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-connect-ai-openai-plugin"))
}
