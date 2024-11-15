import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Bill Of Materials (BOM)"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name != project.name }
            .filter { hasAnArtifact(it) }
            .sortedBy { it.name }
            .forEach { api(it) }
    }
}

fun hasAnArtifact(it: Project) =
    !it.name.contains("test-function") && !it.name.contains("integration-test") && !it.name.contains("tools")

