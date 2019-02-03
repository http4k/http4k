package org.http4k.contract

import org.http4k.format.Jackson

class SimpleJsonTest : ContractRendererContract(SimpleJson(Jackson))