package org.http4k.config

data class Host(val value: String) {
    init {
        require(value.isNotEmpty()) { "Could not construct Host from '$value'" }
    }

    fun asAuthority() = Authority(this)

    companion object {
        val localhost = Host("localhost")
    }
}
