package org.http4k.filters

import org.http4k.contract.ContractRenderer
import org.http4k.filter.ServerFilters

fun ServerFilters.CatchLensFailure(renderer: ContractRenderer) = CatchLensFailure { renderer.badRequest(it.failures) }
