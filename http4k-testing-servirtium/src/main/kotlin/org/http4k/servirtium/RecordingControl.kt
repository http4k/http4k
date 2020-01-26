package org.http4k.servirtium

import org.http4k.traffic.ByteStorage

interface RecordingControl {
    fun addNote(note: String)

    companion object {
        fun ByteStorage(storage: ByteStorage) = object : RecordingControl {
            override fun addNote(note: String) = storage.accept("## $note\n\n".toByteArray())
        }

        object NoOp : RecordingControl {
            override fun addNote(note: String) {}
        }
    }
}
