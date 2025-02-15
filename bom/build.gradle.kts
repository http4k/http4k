import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Bill Of Materials (BOM)"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name != project.name }
            .filter { shouldBePublished(it) }
            .filterNot { it.name == "http4k-tools" }
            .sortedBy { it.name }
            .forEach { api(it) }
    }
}

fun shouldBePublished(p: Project) = setOf(
    "enterprise", // TODO - remove this to publish
    "example",
    "test-function",
    "integration-test",
).none { p.name.contains(it) }
