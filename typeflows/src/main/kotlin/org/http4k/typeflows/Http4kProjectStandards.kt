package org.http4k.typeflows

import io.typeflows.fs.TypeflowsFSEntry
import io.typeflows.fs.TypeflowsResources
import io.typeflows.util.Builder

/**
 * Standard repository project files and directories for an Http4k project.
 *
 * This class provides a predefined set of files and directories that
 * conform to the recommended structure for Http4k projects, facilitating
 * consistency and best practices across projects.
 */
class Http4kProjectStandards : Builder<TypeflowsFSEntry> {
    override fun build() = TypeflowsResources(Http4kProjectStandards::class.java)
}
