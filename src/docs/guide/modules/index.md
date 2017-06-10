Modularity is at the core of **http4k**. The API is very unopinionated and designed to allow the user to use as much 
or as little of it as desired. 

Minimal usage requires the `http4k-core` module, which has ZERO external dependencies. Other modules only bring in the 
minimum of dependencies in order to work. For instance, the Jetty Server backend `http4k-server-jetty` only brings in 
the `jetty-server` and `jetty-servlet` modules.