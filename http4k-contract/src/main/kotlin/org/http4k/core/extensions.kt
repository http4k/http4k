package org.http4k.core

import org.http4k.contract.ContractRouter

fun Filter.then(router: ContractRouter): ContractRouter = router.copy(filter = this)

