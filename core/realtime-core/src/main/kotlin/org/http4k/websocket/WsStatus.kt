package org.http4k.websocket

data class WsStatus(val code: Int, val description: String) {
    companion object {
        val NORMAL = WsStatus(1000, "Normal")
        val GOING_AWAY = WsStatus(1001, "Going away")
        val PROTOCOL_ERROR = WsStatus(1002, "Protocol error")
        val REFUSE = WsStatus(1003, "Refuse")
        val NOCODE = WsStatus(1005, "No code")
        val ABNORMAL_CLOSE = WsStatus(1006, "Abnormal close")
        val NO_UTF8 = WsStatus(1007, "No UTF8")
        val POLICY_VALIDATION = WsStatus(1008, "Policy validation")
        val TOOBIG = WsStatus(1009, "Too big")
        val EXTENSION = WsStatus(1010, "Extension")
        val UNEXPECTED_CONDITION = WsStatus(1011, "Unexpected condition")
        val TLS_ERROR = WsStatus(1015, "TLS error")
        val NEVER_CONNECTED = WsStatus(-1, "Never connected")
        val BUGGYCLOSE = WsStatus(-2, "Buggy close")
        val FLASHPOLICY = WsStatus(-3, "Flash policy")
    }

    fun description(description: String) = copy(description = description)

    override fun equals(other: Any?): Boolean = other != null && other is WsStatus && other.code == code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = "$code $description"
}
