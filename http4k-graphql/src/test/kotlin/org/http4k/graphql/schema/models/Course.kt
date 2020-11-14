package org.http4k.graphql.schema.models

import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import java.util.concurrent.CompletableFuture.supplyAsync

const val COURSE_LOADER_NAME = "COURSE_LOADER"

val batchCourseLoader = DataLoader<Long, Course?> { ids ->
    supplyAsync {
        Course.search(ids).toMutableList()
    }
}

data class Course(
    val id: Long,
    val name: String? = null,
    val universityId: Long? = null,
    val bookIds: List<Long> = listOf()
) {
    fun university(dataFetchingEnvironment: DataFetchingEnvironment) =
        dataFetchingEnvironment.getDataLoader<Long, University>(UNIVERSITY_LOADER_NAME)
            .load(universityId).get()

    fun books(dataFetchingEnvironment: DataFetchingEnvironment) = dataFetchingEnvironment.listBooks()

    private fun Course.listBooks(dataFetchingEnvironment: DataFetchingEnvironment) = list(dataFetchingEnvironment)

    private fun list(dataFetchingEnvironment: DataFetchingEnvironment): List<Book>? = dataFetchingEnvironment.listBooks()

    private fun DataFetchingEnvironment.listBooks(): List<Book>? =
        getDataLoader<List<Long>, List<Book>>(BATCH_BOOK_LOADER_NAME)
            .load(bookIds).get()

    companion object {
        fun search(ids: List<Long>) = listOf(
            Course(id = 1, name = "Biology 101", universityId = 1, bookIds = listOf(1, 2)),
            Course(id = 2, name = "Cultural Anthropology", universityId = 1),
            Course(id = 3, name = "Computer Science 101", universityId = 1, bookIds = listOf(3, 4)),
            Course(id = 4, name = "Computer Science 101", universityId = 3, bookIds = listOf(3, 4))
        ).filter { ids.contains(it.id) }
    }
}
