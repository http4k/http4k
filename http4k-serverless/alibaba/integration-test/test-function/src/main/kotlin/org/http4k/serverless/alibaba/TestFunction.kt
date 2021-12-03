@file:Suppress("unused")

package org.http4k.serverless.alibaba

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.AlibabaCloudHttpFunction

class TestFunction : AlibabaCloudHttpFunction(ServerForClientContract)
