import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI LLM common abstractions"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-ai-core"))

    api("se.ansman.kotshi:api:_")
}
