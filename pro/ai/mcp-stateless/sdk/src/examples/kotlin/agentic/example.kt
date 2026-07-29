/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package agentic

import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun main() {
    al().asServer(JettyLoom(12000)).start()
    david().asServer(JettyLoom(13000)).start()
    franck().asServer(JettyLoom(14000)).start()
    frenchRestaurant().asServer(JettyLoom(15000)).start()
}
