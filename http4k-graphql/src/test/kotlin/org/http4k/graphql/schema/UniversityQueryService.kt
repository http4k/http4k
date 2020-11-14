package org.http4k.graphql.schema

import org.http4k.graphql.schema.models.University

class UniversityQueryService {
    @Suppress("unused")
    fun searchUniversities(params: UniversitySearchParameters) =
        University.search(params.ids)
}

data class UniversitySearchParameters(val ids: List<Long>)
