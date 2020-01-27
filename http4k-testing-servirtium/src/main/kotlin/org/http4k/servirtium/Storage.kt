package org.http4k.servirtium

import java.io.File
import java.util.function.Consumer
import java.util.function.Supplier

interface Storage : Supplier<ByteArray>, Consumer<ByteArray>

interface StorageFactory : (String) -> Storage {
    companion object {
        fun Disk(root: File = File(".")) = object : StorageFactory {
            override fun invoke(name: String): Storage {
                val file = File(root, "$name.md").apply { delete() }
                return object : Storage {
                    override fun get() = file.readBytes()
                    override fun accept(data: ByteArray) = file.appendBytes(data)
                }
            }
        }

        fun InMemory() = object : StorageFactory {
            override fun invoke(name: String): Storage = object : Storage {
                private var bytes = ByteArray(0)
                override fun get() = bytes
                override fun accept(data: ByteArray) {
                    bytes += data
                }
            }
        }
    }
}
