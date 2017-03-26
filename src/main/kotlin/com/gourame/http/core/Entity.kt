package com.gourame.http.core

data class Entity(val value: ByteArray) {
    constructor(value: String) : this(value.toByteArray())

    override fun equals(other: Any?): Boolean = other != null && other is Entity && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = String(value)
}