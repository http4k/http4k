package org.reekwest.http.contract.spike

import org.reekwest.http.contract.ContractBreach

class PathExtractor(private val lenses: List<PathLens<*>>) {
    fun from(path: PathBuilder) = try {
        if (path.toList().size == lenses.size) {
            ExtractedParts(mapOf(*lenses
                .mapIndexed { index, lens -> lens to path(index, lens) }.
                toTypedArray()))
        } else {
            null
        }
    } catch (e: ContractBreach) {
        null
    }
}
