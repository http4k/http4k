package org.http4k.storage

/**
 * Debug all inputs and outputs of storage calls.
 */
fun <T : Any> Storage<T>.debug(printFn: (String) -> Unit = ::println) = object : Storage<T> by this {
    private fun println(s: String) = printFn(this@debug.javaClass.name + " > " + s)

    override fun get(key: String) = this@debug[key].also { println("get: $key > $it") }

    override fun set(key: String, data: T) {
        this@debug[key] = data
        println("set $key >> $data")
    }

    override fun keySet(keyPrefix: String) = this@debug.keySet(keyPrefix).also { println("keySet: $keyPrefix > $it") }

    override fun remove(key: String) = this@debug.remove(key).also { println("remove: $key > $it") }

    override fun removeAll(keyPrefix: String) =
        this@debug.removeAll(keyPrefix).also { println("removeAll: $keyPrefix > $it") }
}
