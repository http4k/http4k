@file:Suppress("unused")

package org.http4k.serverless.fn

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.FnProjectFunction

class TestFunction : FnProjectFunction(ServerForClientContract)
