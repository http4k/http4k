package org.reekwest.http.contract

interface GetLens<in IN, out OUT> {
    operator fun invoke(target: IN): List<OUT>
}

interface SetLens<IN, in OUT> {
    operator fun invoke(values: List<OUT>, target: IN): IN
}

interface BiDiLens<IN, OUT> : GetLens<IN, OUT>, SetLens<IN, OUT>
