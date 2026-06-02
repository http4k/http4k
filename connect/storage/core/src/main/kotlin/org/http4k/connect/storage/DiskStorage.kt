package org.http4k.connect.storage

import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import java.io.File

/**
 * Simple On-Disk, file-backed storage implementation.
 */
inline fun <reified T : Any> Storage.Companion.Disk(dir: File, autoMarshalling: AutoMarshalling = Moshi) =
    object : Storage<T> {
        private val dirRoot = dir.canonicalFile
        private val dirRootPrefix = dirRoot.path + File.separator

        private fun resolveKey(key: String): File? {
            val resolved = runCatching { File(dir, key).canonicalFile }.getOrNull() ?: return null
            return resolved.takeIf { it == dirRoot || it.path.startsWith(dirRootPrefix) }
        }

        override fun get(key: String): T? =
            resolveKey(key)?.takeIf { it.exists() }?.readText()?.let { autoMarshalling.asA<T>(it) }

        override fun set(key: String, data: T) {
            resolveKey(key)?.writeText(autoMarshalling.asFormatString(data))
        }

        override fun remove(key: String) = resolveKey(key)?.delete() == true

        override fun removeAll(keyPrefix: String): Boolean {
            val files = listKeysWith(keyPrefix)
            return when {
                files.isEmpty() -> false
                else -> {
                    files.forEach { it.delete() }
                    true
                }
            }
        }

        private fun listKeysWith(keyPrefix: String): List<File> =
            (dir.listFiles { pathname -> pathname.isFile && pathname.name.startsWith(keyPrefix) }
                ?: emptyArray()).toList()

        override fun keySet(keyPrefix: String) = listKeysWith(keyPrefix).map { it.name }.toSet()

        override fun toString() = listKeysWith("").joinToString(",") { it.name }
    }
