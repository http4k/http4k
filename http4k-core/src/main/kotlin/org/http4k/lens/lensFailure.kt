package org.http4k.lens

class LensFailure(val failures: List<Failure>, override val cause: Exception? = null, val target: Any? = null) : Exception(failures.joinToString { it.toString() }, cause) {

    constructor(vararg failures: Failure, cause: Exception? = null, target: Any? = null) : this(failures.asList(), cause, target)

    fun overall(): Failure.Type = with(failures.map(Failure::type)) {
        when {
            contains(Failure.Type.Unsupported) -> Failure.Type.Unsupported
            isEmpty() || contains(Failure.Type.Invalid) -> Failure.Type.Invalid
            else -> Failure.Type.Missing
        }
    }
}

sealed class Failure(val type: Type) {
    enum class Type {
        Invalid, Missing, Unsupported
    }

    abstract val meta: Meta
}

data class Missing(override val meta: Meta) : Failure(Type.Missing) {
    override fun toString(): String = "${meta.location} '${meta.name}' is required"
}

data class Invalid(override val meta: Meta) : Failure(Type.Invalid) {
    override fun toString(): String = "${meta.location} '${meta.name}' must be ${meta.paramMeta.description}"
}

data class Unsupported(override val meta: Meta) : Failure(Type.Unsupported) {
    override fun toString(): String = "${meta.location} '${meta.name}' is not acceptable"
}
