package org.http4k.connect.mcp

import org.http4k.core.Uri
import org.http4k.sse.pipeSseTraffic

fun main() {
    pipeSseTraffic(Uri.of("http://localhost:3001/sse"))
}


//{"jsonrpc":"2.0","result":{"capabilities":{"tools":{"listChanged":true},"prompts":{"listChanged":true},"resources":{"subscribe":true,"listChanged":true},"experimental":{},"logging":{},"sampling":{}},"serverInfo":{"name":"mcp-kotlin test server","version":"bar"},"protocolVersion":"2024-11-05","_meta":{}},"id":10}
