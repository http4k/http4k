package org.http4k.server;

import kotlin.jvm.functions.Function1;
import org.http4k.core.Body;
import org.http4k.core.Request;
import org.http4k.core.Response;

import static org.http4k.core.Status.ACCEPTED;
import static org.http4k.server.Http4kServerKt.asServer;

public interface UsageFromJava_netty {
    Netty netty = new Netty(8000);
    Response response = Response.Companion.create(ACCEPTED).body(Body.create("hello"));
    Function1<Request, Response> httpHandler = req -> response;
    Http4kServer http4kServer = asServer(httpHandler, netty);
}

