import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bill Of Materials (BOM)"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name != project.name }
            .filter { shouldBePublished(it) }
            .sortedBy { it.name }
            .forEach { api(it) }
    }
}

fun shouldBePublished(p: Project) = setOf(
    "enterprise", // TODO - remove this to publish
    "example",
    "test-function",
    "integration-test",
    "tools"
).none { p.name.contains(it) }
