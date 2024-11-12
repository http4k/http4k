description = "Http4k Bill Of Materials (BOM)"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
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
    !it.name.contains("test-function") && !it.name.contains("integration-test")

