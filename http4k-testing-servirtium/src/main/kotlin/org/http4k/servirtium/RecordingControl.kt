package org.http4k.servirtium

interface RecordingControl {
    fun addNote(note: String)

    companion object {
        fun ByteStorage(storage: Storage) = object : RecordingControl {
            override fun addNote(note: String) = storage.accept("## $note\n\n".toByteArray())
        }

        object NoOp : RecordingControl {
            override fun addNote(note: String) {}
        }
    }
}
