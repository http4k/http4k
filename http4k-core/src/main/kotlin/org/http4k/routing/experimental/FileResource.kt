package org.http4k.routing.experimental

import java.io.File
import java.io.InputStream
import java.time.Instant


data class FileResource(val file: File) : Resource {

    override fun openStream(): InputStream = file.inputStream()

    override val length: Long get() = file.length()

    override val lastModified: Instant get() = Instant.ofEpochMilli(file.lastModified())

}