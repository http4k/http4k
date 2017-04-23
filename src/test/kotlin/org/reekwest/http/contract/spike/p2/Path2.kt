package org.reekwest.http.contract.spike.p2


sealed class APath {
    abstract val parent: APath
    abstract fun toList(): List<String>
    abstract fun startsWith(other: APath): Boolean

    operator fun div(child: String): APath = Slash(this, child)

    companion object {
        operator fun invoke(str: String): APath =
            if (str == "" || str == "/")
                Root
            else if (!str.startsWith("/"))
                APath("/" + str)
            else {
                val slash = str.lastIndexOf('/')
                val prefix = APath(str.substring(0, slash))
                if (slash == str.length - 1) prefix else prefix / str.substring(slash + 1)
            }
    }
}

data class Slash(override val parent: APath, val child: String) : APath() {
    override fun toList(): List<String> = parent.toList().plus(child)
    override fun toString(): String = "$parent/$child"
    override  fun startsWith(other: APath): Boolean {
        val components = other.toList()
        return toList().take(components.size) == components
    }
}

object Root : APath() {
    override fun toList(): List<String> = emptyList()
    override val parent: APath = this
    override fun toString(): String = ""
    override fun startsWith(other: APath): Boolean = other == Root
}
