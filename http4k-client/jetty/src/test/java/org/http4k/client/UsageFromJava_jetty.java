package org.http4k.client;

import kotlin.jvm.functions.Function1;
import org.http4k.core.Request;
import org.http4k.core.Response;

public interface UsageFromJava_jetty {
    Function1<Request, Response> apache = JettyClient.create();
}
