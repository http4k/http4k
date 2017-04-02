package org.reekwest.http.core

data class Entity(val value: ByteArray) {
    constructor(value: String) : this(value.toByteArray())

    override fun equals(other: Any?): Boolean = other != null && other is Entity && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = String(value)
}

interface EntityExtractor<out T> : (HttpMessage) -> T

object StringEntity : EntityExtractor<String> {
    override fun invoke(request: HttpMessage): String = request.entity?.toString() ?: ""
}

fun <T> HttpMessage.extract(entityExtractor: EntityExtractor<T>): T = entityExtractor(this)