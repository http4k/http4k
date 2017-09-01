package org.http4k.lens

data class LensFailure(val failures: List<Failure>, override val cause: Exception? = null) : Exception(failures.map { it.toString() }.joinToString(), cause) {

    constructor(vararg failures: Failure, cause: Exception? = null) : this(failures.asList(), cause)

    fun overall(): Failure.Type {
        val all = failures.map { it.type }
        return if (all.contains(Failure.Type.Unsupported)) Failure.Type.Unsupported
        else if (all.isEmpty() || all.contains(Failure.Type.Invalid)) Failure.Type.Invalid
        else Failure.Type.Missing
    }

    companion object {
        operator fun invoke(vararg failures: Failure, cause: Exception? = null) = LensFailure(failures.toList(), cause)
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
    override fun toString(): String = "${meta.location} '${meta.name}' must be ${meta.paramMeta.value}"
}

data class Unsupported(override val meta: Meta) : Failure(Type.Unsupported) {
    override fun toString(): String = "${meta.location} '${meta.name}' is not acceptable"
}
