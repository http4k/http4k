/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

enum class SettlementMode {
    /** Verify → Settle → run tool. If Settle fails, the tool is never invoked. */
    SettleBefore,

    /** Verify → run tool → Settle. The tool effect happens even if Settle later fails. */
    SettleAfter
}
