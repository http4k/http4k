package org.http4k.internal

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ProjectMetadata : Plugin<Project> {
    override fun apply(project: Project) {
       project.extensions.create("metadata", Extension::class.java)
    }

    open class Extension {
        var developers: Map<String, String> = emptyMap()
    }
}

