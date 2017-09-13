Find here answers to the most common questions that we get asked about **http4k**

Q. Auto-marshalling: where is the `Body.auto` method defined?
A. `Body.auto` is an extension method which is declared on the parent singleton `object` for each of the message libraries that supports auto-marshalling - eg. `Jackson`, `Gson` and `Xml`. All of these objects are declared in the same package, so you need to add an import similar to:
`import org.http4k.format.Jackson.auto`

Q. My application uses Lenses, but when they fail I get an HTTP 500 instead of the promised 400.
A. You forgot to add the `ServerFilters.CatchLensFailure` filter to your application stack.

Q. Where are all the useful Filters defined?
A. Filters are all in the `import org.http4k.filter` package and are located as methods on a singleton `object` relevant to their use:
  - `org.http4k.filter.CachingFilters.Request` & `org.http4k.filter.CachingFilters.Response` 
  - `org.http4k.filter.ClientFilters`
  - `org.http4k.filter.ServerFilters` 
  - `org.http4k.filter.DebuggingFilters`
