import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bill Of Materials (BOM)"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    constraints {
        rootProject.subprojects
            .asSequence()
            .filter { it.name != project.name }
            .filter { shouldBePublished(it) }
            .filterNot { it.name.contains("http4k-mcp") }
            .filterNot { it.name == "http4k-tools" }
            .sortedBy { it.name }
            .toList()
            .forEach { api(it) }
    }
}

fun shouldBePublished(p: Project) = setOf(
    "example",
    "test-function",
    "integration-test",
).none { p.name.contains(it) }
