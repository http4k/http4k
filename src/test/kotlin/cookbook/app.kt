package cookbook


//fun main(args: Array<String>) {
//
//    fun add(value1: Int, value2: Int): HttpHandler = {
//        Response(OK).with(
//            Body.string.required() to (value1 + value2).toString()
//        )
//    }
//
//    fun echo(name: String, age: Int): HttpHandler = {
//        Response(OK).with(
//            Body.string.required() to "hello $name you are $age"
//        )
//    }
//
//    val handler = RouteModule(Root / "foo", SimpleJson())
////        .securedBy(ApiKey(Query.int().required("api"), { it == 42 }))
//        .withRoute(Route("add").at(GET) / "add" / Path.int().of("value1") / Path.int().of("value2") bind ::add)
//        .withRoute(Route("echo").at(GET) / "echo" / Path.of("name") / Path.int().of("age") bind ::echo)
//        .toHttpHandler()
//
//    handler.asJettyServer(8000).start().block()
//
//}
