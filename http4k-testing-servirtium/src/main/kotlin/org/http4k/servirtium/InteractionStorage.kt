package org.http4k.servirtium

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

typealias InteractionStorageLookup = (String) -> InteractionStorage

/**
 * Provides storage for the recorded Servirtium interaction data.
 */
interface InteractionStorage : Supplier<ByteArray>, Consumer<ByteArray> {
    fun clean(): Boolean

    companion object {
        fun Disk(root: File = File(".")): InteractionStorageLookup = object : InteractionStorageLookup {
            override fun invoke(name: String): InteractionStorage {
                val file = fileFor(name)
                return object : InteractionStorage {
                    override fun get() = file.takeIf { it.exists() }?.readBytes() ?: ByteArray(0)
                    override fun accept(data: ByteArray) {
                        file.appendBytes(data)
                    }

                    override fun clean(): Boolean = fileFor(name).delete()
                }
            }

            private fun fileFor(name: String) = File(root, "$name.md")
        }

        fun InMemory() = object : InteractionStorageLookup {
            private val created = mutableMapOf<String, AtomicReference<ByteArray>>()

            override fun invoke(name: String): InteractionStorage {
                val ref = created[name] ?: AtomicReference(ByteArray(0))
                created[name] = ref
                return object : InteractionStorage {
                    override fun get() = ref.get()
                    override fun accept(data: ByteArray) {
                        ref.set(ref.get() + data)
                    }

                    override fun clean() = created[name]?.let { it.set(ByteArray(0)); true } ?: false
                }
            }
        }
    }
}
