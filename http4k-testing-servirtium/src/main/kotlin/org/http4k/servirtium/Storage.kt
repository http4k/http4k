package org.http4k.servirtium

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

interface Storage : Supplier<ByteArray>, Consumer<ByteArray>

interface StorageFactory : (String) -> Storage {
    fun clean(name: String): Boolean

    companion object {
        fun Disk(root: File = File(".")) = object : StorageFactory {
            override fun invoke(name: String): Storage {
                val file = fileFor(name)
                return object : Storage {
                    override fun get() = file.takeIf { it.exists() }?.readBytes() ?: ByteArray(0)
                    override fun accept(data: ByteArray) {
                        file.appendBytes(data)
                    }
                }
            }

            override fun clean(name: String): Boolean = fileFor(name).delete()

            private fun fileFor(name: String) = File(root, "$name.md")
        }

        fun InMemory() = object : StorageFactory {
            private val created = mutableMapOf<String, AtomicReference<ByteArray>>()

            override fun invoke(name: String): Storage {
                val ref = created[name] ?: AtomicReference(ByteArray(0))
                created[name] = ref
                return object : Storage {
                    override fun get() = ref.get()
                    override fun accept(data: ByteArray) {
                        ref.set(ref.get() + data)
                    }
                }
            }

            override fun clean(name: String) = created[name]?.let { it.set(ByteArray(0)); true } ?: false
        }
    }
}
