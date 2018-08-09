package org.http4k.server

import org.http4k.client.JavaHttpClient

class SunHttpTest : ServerContract(::SunHttp, JavaHttpClient())