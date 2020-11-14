package org.http4k.graphql.schema

import org.http4k.graphql.schema.models.Course

class CourseQueryService {
    @Suppress("unused")
    fun searchCourses(params: CourseQuery) = Course.search(params.ids)
}

data class CourseQuery(val ids: List<Long>)
