package org.http4k.graphql.schema.models

import graphql.GraphQLException
import org.dataloader.DataLoader
import java.util.concurrent.CompletableFuture

const val UNIVERSITY_LOADER_NAME = "UNIVERSITY_LOADER"

val batchUniversityLoader = DataLoader<Long, University?> { ids ->
    CompletableFuture.supplyAsync {
        University.search(ids)
    }
}

class University(val id: Long, val name: String? = null) {
    companion object {
        fun search(ids: List<Long>): List<University> =
            listOf(
                University(id = 1, name = "University of Nebraska-Lincoln"),
                University(id = 2, name = "Kansas State University"),
                University(id = 3, name = "Purdue University"),
                University(id = 4, name = "Kennesaw State University"),
                University(id = 5, name = "University of Georgia")
            ).filter { ids.contains(it.id) }
    }

    fun longThatNeverComes(): Long {
        throw GraphQLException("This value will never return")
    }
}
