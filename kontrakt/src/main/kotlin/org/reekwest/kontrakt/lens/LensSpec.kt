package org.reekwest.kontrakt.lens

interface MultiLensSpec<in IN, out OUT> {
    fun optional(name: String, description: String? = null): Lens<IN, List<OUT>?>
    fun required(name: String, description: String? = null): Lens<IN, List<OUT>>
}

open class LensSpec<IN, MID, out OUT>(protected val location: String, protected val get: Get<IN, MID, OUT>) {
    fun <NEXT> map(nextIn: (OUT) -> NEXT) = LensSpec(location, get.map(nextIn))

    open fun optional(name: String, description: String? = null): Lens<IN, OUT?> {
        val meta = Meta(false, location, name, description)
        val getLens = get(name)
        return Lens(meta, { getLens(it).firstOrNull() })
    }

    open fun required(name: String, description: String? = null): Lens<IN, OUT> {
        val meta = Meta(true, location, name, description)
        val getLens = get(name)
        return Lens(meta, { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) })
    }

    open val multi = object : MultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): Lens<IN, List<OUT>?> {
            val meta = Meta(false, location, name, description)
            val getLens = get(name)
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) null else it } })
        }

        override fun required(name: String, description: String?): Lens<IN, List<OUT>> {
            val meta = Meta(true, location, name, description)
            val getLens = get(name)
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it } })
        }
    }
}

interface BiDiMultiLensSpec<in IN, OUT> : MultiLensSpec<IN, OUT> {
    override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?>
    override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>>
}

open class BiDiLensSpec<IN, MID, OUT>(location: String, get: Get<IN, MID, OUT>,
                                      private val set: Set<IN, MID, OUT>) : LensSpec<IN, MID, OUT>(location, get) {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiLensSpec(location, get.map(nextIn), set.map(nextOut))

    override fun optional(name: String, description: String?): BiDiLens<IN, OUT?> {
        val meta = Meta(false, location, name, description)
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(meta,
            { getLens(it).firstOrNull() },
            { out: OUT?, target: IN -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }

    override fun required(name: String, description: String?): BiDiLens<IN, OUT> {
        val meta = Meta(true, location, name, description)
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) },
            { out: OUT, target: IN -> setLens(listOf(out), target) })
    }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?> {
            val meta = Meta(false, location, name, description)
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(meta,
                { getLens(it).let { if (it.isEmpty()) null else it } },
                { out: List<OUT>?, target: IN -> setLens(out ?: emptyList(), target) }
            )
        }

        override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>> {
            val meta = Meta(true, location, name, description)
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(meta,
                { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it } },
                { out: List<OUT>, target: IN -> setLens(out, target) })
        }
    }
}
