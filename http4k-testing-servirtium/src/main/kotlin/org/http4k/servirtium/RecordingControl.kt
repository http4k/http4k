package org.http4k.servirtium

import java.io.File

interface RecordingControl {
    fun addNote(note: String)

    companion object {
        fun Disk(file: File) = object : RecordingControl {
            override fun addNote(note: String) = file.appendText("## $note\n\n")
        }

        object NoOp : RecordingControl {
            override fun addNote(note: String) {}
        }
    }
}
