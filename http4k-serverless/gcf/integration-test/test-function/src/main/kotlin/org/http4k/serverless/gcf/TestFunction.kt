@file:Suppress("unused")

package org.http4k.serverless.gcf

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.GoogleCloudHttpFunction

class TestFunction : GoogleCloudHttpFunction(ServerForClientContract)
