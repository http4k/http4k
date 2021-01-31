package org.http4k.sse

data class SseStatus(val code: Int, val description: String) {
    companion object {
        val NORMAL = SseStatus(1000, "Normal")
    }

    fun description(description: String) = copy(description = description)

    override fun equals(other: Any?): Boolean = other != null && other is SseStatus && other.code == code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = "$code $description"
}
