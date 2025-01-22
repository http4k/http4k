package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.db.Transactor.Mode.ReadWrite
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.BiDiLens
import org.http4k.lens.RequestContextKey

interface Transactor<out Resource> {
    enum class Mode { ReadOnly, ReadWrite }

    fun <T> perform(mode: Mode = ReadWrite, work: (Resource) -> T): T
}


