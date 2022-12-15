package org.http4k.contract

import org.http4k.core.Request

sealed class PathSegments {
    abstract val parent: PathSegments
    abstract fun toList(): List<String>

    abstract fun startsWith(other: PathSegments): Boolean
    operator fun div(child: String): PathSegments = Slash(this, child)
    operator fun div(child: PathSegments): PathSegments = child.toList().fold(this) { memo, next ->
        memo / next
    }

    companion object {
        operator fun invoke(str: String): PathSegments =
            if (str == "" || str == "/") Root
            else if (!str.startsWith("/")) PathSegments("/$str")
            else {
                val slash = str.lastIndexOf('/')
                val prefix = PathSegments(str.substring(0, slash))
                if (slash == str.length - 1) prefix else prefix / str.substring(slash + 1)
            }
    }
}

data class Slash(override val parent: PathSegments, private val child: String) : PathSegments() {
    override fun toList(): List<String> = parent.toList().plus(child)
    override fun toString(): String = "$parent/$child"
    override fun startsWith(other: PathSegments): Boolean = other.toList().let { toList().take(it.size) == it }
}

object Root : PathSegments() {
    override fun toList(): List<String> = emptyList()
    override val parent: PathSegments = this
    override fun toString(): String = ""
    override fun startsWith(other: PathSegments): Boolean = other == Root
}

internal fun Request.isIn(contractRoot: PathSegments) = pathSegments().startsWith(contractRoot)

internal fun Request.pathSegments() = PathSegments(uri.path)
internal fun Request.without(pathSegments: PathSegments) =
    PathSegments(uri.path.replaceFirst(pathSegments.toString(), ""))
