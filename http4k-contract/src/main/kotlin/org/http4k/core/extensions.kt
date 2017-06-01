package org.http4k.core

import org.http4k.contract.ContractRouter
import org.http4k.filter.ServerFilters

fun Filter.then(router: ContractRouter): ContractRouter = router.copy(filter = ServerFilters.CatchLensFailure.then(this))

