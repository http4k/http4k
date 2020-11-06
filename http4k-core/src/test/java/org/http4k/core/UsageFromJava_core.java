package org.http4k.core;

import kotlin.jvm.functions.Function1;

import static org.http4k.core.Http4kKt.then;
import static org.http4k.core.Status.ACCEPTED;
import static org.http4k.routing.RoutingKt.bind;
import static org.http4k.routing.RoutingKt.routes;


public interface UsageFromJava_core {
    Uri hello = Uri.of("hello");
    Request request = Request.Companion.create(Method.GET, "").body(Body.EMPTY);
    Response response = Response.Companion.create(ACCEPTED).body(Body.create("hello"));
    Function1<Request, Response> httpHandler = req -> response;
    Filter filter = next -> req -> next.invoke(request.header("foo", "bar"));

    HttpHandler decorated = then(filter, then(filter, httpHandler));
    Response response2 = decorated.invoke(request);

    HttpHandler app = routes(
        bind("/nested", routes(
            bind("/second", Method.GET).to(req -> response2)
        )),
        bind("/first", Method.GET).to(req -> response)
    );
}
