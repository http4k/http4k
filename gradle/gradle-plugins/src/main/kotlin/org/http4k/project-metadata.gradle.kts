package org.http4k

import org.http4k.internal.ProjectMetadata

plugins {
    id("org.jetbrains.dokka")
}

// This is a workaround for the dokka plugin adding dependencies to the project instead of the plugin
repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "org.http4k"

apply<ProjectMetadata>()

if(rootProject.tasks.findByName("listProjects") == null) {
    rootProject.tasks.register("listProjects") {
        doLast {
            allprojects
                .filter { it.tasks.findByName("publishToSonatype") != null }
                .map { it.name }
                .forEach(System.err::println)
        }
    }
}

