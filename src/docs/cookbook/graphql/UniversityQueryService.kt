package cookbook.graphql

class UniversityQueryService {
    @Suppress("unused")
    fun searchUniversities(params: UniversitySearchParameters) =
        University.search(params.ids)
}

data class UniversitySearchParameters(val ids: List<Long>)
