package org.reekwest.kontrakt.module

import org.reekwest.http.core.Request

sealed class BasePath {
    abstract val parent: BasePath
    abstract fun toList(): List<String>

    abstract fun startsWith(other: BasePath): Boolean
    operator fun div(child: String): BasePath = Slash(this, child)

    companion object {
        operator fun invoke(str: String): BasePath =
            if (str == "" || str == "/") Root
            else if (!str.startsWith("/")) BasePath("/" + str)
            else {
                val slash = str.lastIndexOf('/')
                val prefix = BasePath(str.substring(0, slash))
                if (slash == str.length - 1) prefix else prefix / str.substring(slash + 1)
            }
    }
}

data class Slash(override val parent: BasePath, val child: String) : BasePath() {
    override fun toList(): List<String> = parent.toList().plus(child)
    override fun toString(): String = "$parent/$child"
    override fun startsWith(other: BasePath): Boolean = other.toList().let {toList().take(it.size) == it}
}

object Root : BasePath() {
    override fun toList(): List<String> = emptyList()
    override val parent: BasePath = this
    override fun toString(): String = ""
    override fun startsWith(other: BasePath): Boolean = other == Root
}

internal fun Request.isIn(moduleRoot: BasePath) = basePath().startsWith(moduleRoot)

internal fun Request.basePath() = BasePath(uri.path)
