package org.http4k.db

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.db.testing.AccountRepository
import org.http4k.db.testing.PlainSqlAccountRepository
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

class TransactorFilterTest {
}

fun main() {
    val contexts = RequestContexts()
    val transactor = DataSourceTransactor<AccountRepository>(createDataSource(), ::PlainSqlAccountRepository)

    val app = routes(
        "/balance/{account}" bind GET to
            { req: Request ->
                val repository: AccountRepository = req.transactionResource(contexts)

                val balance = repository.getBalance(req.path("account")!!)
                Response(Status.OK).body(balance.toString())
            })

    Filter.NoOp
        .then(TransactionPerRequestFilter(contexts, transactor))
        .then(app)
        .asServer(SunHttp(8000))
        .start()

    JavaHttpClient()(Request(GET, "http://localhost:8000/balance/DEPOSITS")).let(::println)
}
