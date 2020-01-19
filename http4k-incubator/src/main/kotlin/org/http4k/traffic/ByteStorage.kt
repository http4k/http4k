package org.http4k.traffic

import java.io.File
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Generic wrapper interface to provide data storage
 */
interface ByteStorage : Supplier<ByteArray>, Consumer<ByteArray> {
    companion object {
        fun Disk(file: File, clean: Boolean = false) = object : ByteStorage {
            override fun get() = file.readBytes()
            override fun accept(data: ByteArray) = file.apply { if (clean) delete() }.appendBytes(data)
        }
    }
}
