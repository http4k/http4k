package org.reekwest.http.contract.spike


sealed class BasePath {
    abstract val parent: BasePath
    abstract fun toList(): List<String>
    operator fun <T> invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

//    abstract fun startsWith(other: PathBuilder): Boolean

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
//    override fun startsWith(other: PathBuilder): Boolean {
//        val components = other.toList()
//        return toList().take(components.size) == components
//    }
}

object Root : BasePath() {
    override fun toList(): List<String> = emptyList()
    override val parent: BasePath = this
    override fun toString(): String = ""
//    override fun startsWith(other: PathBuilder): Boolean = other == Root
}
