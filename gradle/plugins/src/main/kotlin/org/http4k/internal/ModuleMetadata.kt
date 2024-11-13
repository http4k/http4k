package org.http4k.internal

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ModuleMetadata : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metadata", Extension::class.java)
    }

    open class Extension {
        lateinit var license: License
    }

    data class License(val name: String, val url: String)
}

