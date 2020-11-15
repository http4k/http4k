package cookbook.graphql

class CourseQueryService {
    @Suppress("unused")
    fun searchCourses(params: CourseQuery) = Course.search(params.ids)
}

data class CourseQuery(val ids: List<Long>)
