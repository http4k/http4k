package org.http4k.connect.storage

/**
 * Storage for a set of objects keyed by String
 */
interface Storage<T : Any> {

    /**
     * Get a value from the storage by key
     */
    operator fun get(key: String): T?

    /**
     * Set a value in the storage against a key
     */
    operator fun set(key: String, data: T)

    /**
     * Remove the object from the storage by key
     */
    operator fun minusAssign(key: String) {
        remove(key)
    }

    /**
     * Remove the object from the storage by key
     */
    fun remove(key: String): Boolean

    /**
     * Get all of the keys avaulable in the storage
     */
    fun keySet(keyPrefix: String = ""): Set<String>

    /**
     * Remove all objects from the storage which match the keyPrefix
     */
    fun removeAll(keyPrefix: String = ""): Boolean

    companion object
}
