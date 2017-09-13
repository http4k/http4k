Find here answers to the most common questions that we get asked about **http4k**

### General
**Q. Is **http4k** a library or a framework?**

**A.** Although it has many of the features of a framework, we consider http4k to be a library which adds a common HTTP routing layer. Is it incredibly unopinionated and has been designed to not enforce design decisions on the API user. We use http4k for applications both large and small, using no DI framework.

**Q. Is **http4k** currently used in production?**

**A.** Absolutely! The library is currently in use serving the global traffic for a large academic publisher (easily serving 10s of millions of requests per day on a few nodes) and is also being used in production in at least 2 global Investment Banks (that we know of). If you're running http4k in production and would like to be listed on the site as an adpoter, please get in touch.

**Q. Auto-marshalling: where is the `Body.auto` method defined?**

**A.** `Body.auto` is an extension method which is declared on the parent singleton `object` for each of the message libraries that supports auto-marshalling - eg. `Jackson`, `Gson` and `Xml`. All of these objects are declared in the same package, so you need to add an import similar to:
`import org.http4k.format.Jackson.auto`

**Q. My application uses Lenses, but when they fail I get an HTTP 500 instead of the promised 400.**
**A.** You forgot to add the `ServerFilters.CatchLensFailure` filter to your application stack.

**Q. Where are all the useful Filters defined?**
**A.** Filters are all in the `import org.http4k.filter` package and are located as methods on a singleton `object` relevant to their use:
  - `org.http4k.filter.CachingFilters.Request` & `org.http4k.filter.CachingFilters.Response` 
  - `org.http4k.filter.ClientFilters`
  - `org.http4k.filter.ServerFilters` 
  - `org.http4k.filter.DebuggingFilters`

**Q. Does http4k support an Async model? I need webscale!**
**A.** Not at the moment. Adding Async support is a decision that we are thinkning about carefully so that we don't end up complicating the API. When we do add it, it'll probably use co-routines and they're still marked as experimental which is another reason we are holding off. As for the scaling arguments, see the above answer relating to production usage.
