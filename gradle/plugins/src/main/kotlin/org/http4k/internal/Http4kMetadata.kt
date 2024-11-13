package org.http4k.internal

import org.gradle.api.Plugin
import org.gradle.api.Project

open class Http4kMetadata : Plugin<Project> {
    override fun apply(project: Project) {
       project.extensions.create("http4kMetadata", MetaDataExtension::class.java)
    }
}

open class MetaDataExtension {
    var developers: Map<String, String> = emptyMap()
}
