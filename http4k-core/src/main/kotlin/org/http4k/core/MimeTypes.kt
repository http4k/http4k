package org.http4k.core

import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.util.loadMetaResource

class MimeTypes private constructor(private val map: Map<String, ContentType>) {
    fun forFile(file: String): ContentType =
        file.drop(file.lastIndexOf('.') + 1).let {
            if (it == file) OCTET_STREAM else map[it.toLowerCase()] ?: OCTET_STREAM
        }

    companion object {
        operator fun invoke(overrides: Map<String, ContentType> = emptyMap()): MimeTypes =
            if (overrides.isEmpty()) standardTypes else MimeTypes(standardTypes.map + overrides)

        private val standardTypes: MimeTypes by lazy { MimeTypes(loadStandard()) }

        private fun loadStandard(): Map<String, ContentType> =
            loadMetaResource<MimeTypes>("mime.types").reader().readLines().flatMap { line ->
                line.split('\t')
                    .filter { it.trim().isNotBlank() }
                    .run {
                        if (size != 2) throw RuntimeException("mime.types file is malformed [$line]")
                        this[1].split(" ").map(String::trim).map { it.toLowerCase() to ContentType(this[0]) }
                    }
            }.toMap()
    }
}
