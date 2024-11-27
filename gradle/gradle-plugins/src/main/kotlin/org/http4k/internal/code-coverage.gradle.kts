package org.http4k.internal

plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

tasks {
    named<JacocoReport>("jacocoTestReport") {
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named<Test>("test").get() })

    sourceDirectories.from(subprojects.flatMap { it.the<SourceSetContainer>()["main"].allSource.srcDirs })
    classDirectories.from(subprojects.map { it.the<SourceSetContainer>()["main"].output })
    executionData.from(subprojects
        .filter { it.name != "http4k-bom" && hasAnArtifact(it) }
        .map {
            it.tasks.named<JacocoReport>("jacocoTestReport").get().executionData
        }
    )

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(file("${layout.buildDirectory}/reports/jacoco/test/jacocoRootReport.xml"))
    }
}

private fun hasAnArtifact(it: Project) = !it.name.contains("test-function") && !it.name.contains("integration-test")
