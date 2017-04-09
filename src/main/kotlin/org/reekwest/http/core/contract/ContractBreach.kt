package org.reekwest.http.core.contract

sealed class ContractBreach(meta: Meta) : Exception(meta.toString())

class Missing(meta: Meta) : ContractBreach(meta)
class Invalid(meta: Meta) : ContractBreach(meta)
