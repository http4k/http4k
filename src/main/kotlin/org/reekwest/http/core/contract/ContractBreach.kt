package org.reekwest.http.core.contract

sealed class ContractBreach(param: MessagePart<*, *, *>) : Exception(param.toString())

class Missing(param: MessagePart<*, *, *>) : ContractBreach(param)
class Invalid(param: MessagePart<*, *, *>) : ContractBreach(param)

