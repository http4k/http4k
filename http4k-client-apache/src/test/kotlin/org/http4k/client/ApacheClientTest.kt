package org.http4k.client

import org.http4k.server.SunHttp

class ApacheClientTest : Http4kClientContract({ SunHttp(it) }, ApacheClient()){
}
