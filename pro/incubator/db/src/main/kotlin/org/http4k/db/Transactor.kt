package org.http4k.db

import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.db.Transactor.Mode.ReadWrite
import org.http4k.lens.RequestKey

interface Transactor<out Resource> {
    enum class Mode { ReadOnly, ReadWrite }

    fun <T> perform(mode: Mode = ReadWrite, work: (Resource) -> T): T

    companion object {
        private const val KEY = "http4k-db-transactor"

        fun <Resource : Any> transactionResourceFor(request: Request): Resource = RequestKey.required<Resource>(KEY)(request)

        fun <Resource : Any> withTransactionResource(request: Request, resource: Resource): Request =
            request.with(
                RequestKey.required<Resource>(KEY) of resource
            )
    }
}


