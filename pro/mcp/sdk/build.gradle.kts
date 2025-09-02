import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "Deprecated: use http4k-ai-mcp-sdk instead"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-sdk"))
}
