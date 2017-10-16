Find here answers to the most common questions that we get asked about **http4k**

### General
**Q. Is http4k a library or a framework?**

**A.** Although it has many of the features of a framework, we consider http4k to be a library which adds a common HTTP routing layer. Is it incredibly unopinionated and has been designed to not enforce design decisions on the API user. We use http4k for applications both large and small, using no DI framework.

**Q. Is http4k currently used in production?**

**A.** Absolutely! The library is currently in use serving the global traffic for a large academic publisher (easily serving 10s of millions of requests per day on a few nodes) and is also being used in production in at least 2 global Investment Banks (that we know of). If you're running http4k in production and would like to be listed on the site as an adopter, please get in touch.

**Q. Does http4k support an Async model? I need webscale!**

**A.** Not at the moment. Adding Async support is a decision that we are thinking about carefully so that we don't end up complicating the API. When we do add it, it'll probably use co-routines and they're still marked as experimental which is another reason we are holding off. As for the scaling arguments, see the above answer relating to production usage.

### API
**Q. I'm attempting to build HTTP messages using the API, but changes don't affect the object (e.g. calling `request.body("hello")`)?**

**A.** **http4k** HTTP message objects are *immutable*, so you need to chain or reassign the value from the method call to get the updated version.

**Q. Where are all the useful Filters defined?**

**A.** Filters are all in the `import org.http4k.filter` package and are located as methods on a singleton `object` relevant to their use:

- `org.http4k.filter.CachingFilters.Request` & `org.http4k.filter.CachingFilters.Response` 
- `org.http4k.filter.ClientFilters`
- `org.http4k.filter.DebuggingFilters`
- `org.http4k.filter.ServerFilters` 
- `org.http4k.filter.TrafficFilters`

### Lenses & Auto-Marshalling

**Q. Where is the `Body.auto` method defined?**

**A.** `Body.auto` is an extension method which is declared on the parent singleton `object` for each of the message libraries that supports auto-marshalling - eg. `Jackson`, `Gson` and `Xml`. All of these objects are declared in the same package, so you need to add an import similar to:
`import org.http4k.format.Jackson.auto`

**Q. Declared with `Body.auto<List<XXX>>().toLens()`, my auto-marshalled List doesn't extract properly!**

**A.** This is a Jackson-ism. Use `Body.auto<Array<MyIntWrapper>>().toLens()` instead. Yes, it's annoying but we haven't found a way to turn if off.

**Q. Using Jackson, the Data class auto-marshalling is not working correctly when my JSON fields start with capital letters**

**A.** Because of the way in which the Jackson library works, uppercase field names are NOT supported. Either switch out to use `http4k-format-gson` (which has the same API), or annotate your Data class fields with `@JsonAlias` to get the deserialisation to work correctly.

**Q. My application uses Lenses, but when they fail I get an HTTP 500 instead of the promised 400.**

**A.** You forgot to add the `ServerFilters.CatchLensFailure` filter to your Server stack.
