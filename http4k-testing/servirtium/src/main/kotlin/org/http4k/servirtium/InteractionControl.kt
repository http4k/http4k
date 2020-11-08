package org.http4k.servirtium

/**
 * Provides controls for interacting with an in-action Interaction recording.
 */
fun interface InteractionControl {
    fun addNote(note: String)

    companion object {
        @JvmStatic
        fun StorageBased(storage: InteractionStorage) = InteractionControl { storage.accept("## $it\n\n".toByteArray()) }

        val NoOp = InteractionControl { }
    }
}
