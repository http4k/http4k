package org.http4k.db

import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.db.testing.AccountRepository
import org.http4k.db.testing.PlainSqlAccountRepository
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

fun main() {
    val transactor = DataSourceTransactor<AccountRepository>(createDataSource(), ::PlainSqlAccountRepository)

    val app = routes(
        "/balance/{account}" bind GET to
            { req: Request ->
                val repository: AccountRepository = req.transactionResource()

                val balance = repository.getBalance(req.path("account")!!)
                Response(Status.OK).body(balance.toString())
            })

    val server = Filter.NoOp
        .then(TransactionPerRequestFilter(transactor))
        .then(app)

    server(Request(GET, "/balance/DEPOSITS")).let(::println)
}
