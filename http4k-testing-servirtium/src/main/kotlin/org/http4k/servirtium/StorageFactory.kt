package org.http4k.servirtium

import org.http4k.traffic.ByteStorage
import java.io.File

/**
 * Provides storage capability for
 */
interface StorageFactory : (String) -> ByteStorage {
    companion object {
        fun Disk(root: File = File(".")) = object: StorageFactory {
            override fun invoke(name: String): ByteStorage =
                ByteStorage.Disk(File(root, "$name.md").apply { delete() })
        }
    }
}
