package org.http4k.example;

import okhttp3.OkHttpClient;
import org.http4k.client.DualSyncAsyncHttpHandler;
import org.http4k.client.OkHttp;
import org.http4k.core.BodyMode;
import org.http4k.core.Filter;
import org.http4k.core.Request;
import org.http4k.core.Response;
import org.http4k.core.Status;
import org.http4k.routing.RoutingHttpHandler;
import org.http4k.server.Http4kServer;
import org.http4k.server.SunHttp;

import static org.http4k.core.Http4kKt.then;
import static org.http4k.core.Method.POST;
import static org.http4k.routing.RoutingKt.bind;
import static org.http4k.routing.RoutingKt.routes;
import static org.http4k.server.Http4kServerKt.asServer;

public class JavaExample {

    public static void main(String[] args) {
        Filter f = next -> req -> next.invoke(req.body(req.bodyString().substring(0, 5)));

        RoutingHttpHandler routing = routes(bind("/path", POST).to(req -> Response.create(Status.ACCEPTED)));

        RoutingHttpHandler app = then(f, routing);

        Http4kServer http4kServer = asServer(app, new SunHttp(8000));

        http4kServer.start();

        DualSyncAsyncHttpHandler client = OkHttp.create(new OkHttpClient.Builder().build(), BodyMode.Memory.INSTANCE);

        System.out.println(client.invoke(Request.create(POST, "http://localhost:8000/path").body("1234567890")));

        http4kServer.stop();
    }
}
