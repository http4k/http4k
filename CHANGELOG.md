## Changelog

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their rationale when appropriate:

### v1.7.0
- Added auto() to Jackson, so you can auto convert body objects into and out of Requests/Responses

### v1.6.0
- Added CachingFilters

### v1.5.0
- Removed static factory methods for Request/Response. They were confusing/incomplete and users can easily recreate them via extension functions.
- Merge `org.http4k.core.Body` and `org.http4k.lens.Body`.
- Add Request/Response message parsers.

### v1.4.0
- Turn Body into ByteBuffer wrapper rather than typealias. That should make .toString() behave as most people would expected.

### v1.3.0
- Removed non-mandatory parameters from Request and Response constructors. This is aid API clarity.
and force users to use the API methods for properly constructing the objects.
- Regex Lens added.

### v1.0.0
- Initial major release.