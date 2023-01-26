package org.http4k.storage

import org.http4k.format.AutoMarshalling
import java.io.File

/**
 * Simple On-Disk, file-backed storage implementation.
 */
inline fun <reified T : Any> Storage.Companion.Disk(dir: File, autoMarshalling: AutoMarshalling) =
    object : Storage<T> {
        override fun get(key: String): T? =
            File(dir, key).takeIf { it.exists() }?.readText()?.let { autoMarshalling.asA<T>(it) }

        override fun set(key: String, data: T) {
            File(dir, key).writeText(autoMarshalling.asFormatString(data))
        }

        override fun remove(key: String) = File(dir, key).delete()

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
