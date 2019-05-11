package org.http4k.contract.simple

import org.http4k.contract.ContractRendererContract
import org.http4k.format.Jackson

class SimpleJsonTest : ContractRendererContract(SimpleJson(Jackson))