import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bill Of Materials (BOM)"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

val excluded = setOf("http4k-tools", "http4k-incubator-ai-client", "http4k-incubator-db")
dependencies {
    constraints {
        rootProject.subprojects
            .asSequence()
            .filter { it.name != project.name }
            .filter { shouldBePublished(it) }
            .filterNot { excluded.contains(it.name) }
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
