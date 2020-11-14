package org.http4k.graphql.schema

import org.http4k.graphql.schema.models.Course

class CourseQueryService {
    @Suppress("unused")
    fun searchCourses(params: CourseSearchParameters) = Course.search(params.ids)
}

data class CourseSearchParameters(val ids: List<Long>)
