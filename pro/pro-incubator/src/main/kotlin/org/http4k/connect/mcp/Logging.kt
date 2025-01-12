package org.http4k.connect.mcp

object Logging {
    enum class Level {
        debug, info, notice, warning, error, critical, alert, emergency;
    }

    object SetLevel : HasMethod {
        override val Method = McpRpcMethod.of("logging/set_level")

        data class Request(val level: Level, override val _meta: Meta = HasMeta.default) : ClientRequest,
            HasMeta
    }
}
