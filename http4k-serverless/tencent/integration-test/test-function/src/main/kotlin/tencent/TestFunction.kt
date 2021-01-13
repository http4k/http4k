@file:Suppress("unused")

package tencent

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.TencentCloudFunction

class TestFunction : TencentCloudFunction(ServerForClientContract)
