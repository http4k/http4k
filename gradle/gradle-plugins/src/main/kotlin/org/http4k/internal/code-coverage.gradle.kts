package org.http4k.internal

plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

val generatedClasses = "**/Kotshi*"

tasks {
    named<JacocoReport>("jacocoTestReport") {
        classDirectories.setFrom(
            project.the<SourceSetContainer>()["main"].output.asFileTree.matching { exclude(generatedClasses) }
        )

        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }
}

if (project == rootProject) {
    tasks.register<JacocoReport>("jacocoRootReport") {
        val covered = subprojects
            .filter { it.name != "http4k-bom" && hasAnArtifact(it) }
            .mapNotNull { it.tasks.findByName("jacocoTestReport") as JacocoReport? }

        dependsOn(covered.map { it.project.tasks.named<Test>("test").get() })

        sourceDirectories.from(covered.flatMap { it.project.the<SourceSetContainer>()["main"].allSource.srcDirs })
        classDirectories.from(covered.map {
            it.project.the<SourceSetContainer>()["main"].output.asFileTree.matching { exclude(generatedClasses) }
        })
        executionData.from(covered.map { it.executionData })

        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoRootReport.xml"))
        }
    }
}

private fun hasAnArtifact(it: Project) = !it.name.contains("test-function") &&
    !it.name.contains("integration-test") &&
    !it.name.contains("conformance") &&
    !it.name.contains("incubator")
