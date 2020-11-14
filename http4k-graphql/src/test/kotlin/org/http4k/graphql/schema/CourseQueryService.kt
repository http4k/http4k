package org.http4k.graphql.schema

import org.http4k.graphql.schema.models.Course

class CourseQueryService {
    fun searchCourses(params: CourseSearchParameters) = Course.search(params.ids)
}

data class CourseSearchParameters(val ids: List<Long>)
