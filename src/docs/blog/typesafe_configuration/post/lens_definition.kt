package blog.typesafe_configuration.post

interface LensExtractor<in IN, out OUT> : (IN) -> OUT {
    override operator fun invoke(target: IN): OUT
}

interface LensInjector<in IN, in OUT> {
    operator fun <R : OUT> invoke(value: IN, target: R): R
}

interface Lens<IN, OUT> : LensExtractor<IN, OUT>

interface BiDiLens<IN, OUT> : Lens<IN, OUT>, LensInjector<IN, OUT>
