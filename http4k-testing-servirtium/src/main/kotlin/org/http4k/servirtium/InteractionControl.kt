package org.http4k.servirtium

/**
 * Provides controls for interacting with an in-action Interaction recording.
 */
interface InteractionControl {
    fun addNote(note: String)

    companion object {
        fun StorageBased(storage: InteractionStorage) = object : InteractionControl {
            override fun addNote(note: String) = storage.accept("## $note\n\n".toByteArray())
        }

        object NoOp : InteractionControl {
            override fun addNote(note: String) {}
        }
    }
}
