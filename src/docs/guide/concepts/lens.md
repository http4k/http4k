title: http4k Lenses
description: An explanation of Lenses and how they are used in http4k

Lenses provide typesafe parameter destructuring/construction of HTTP messages. Getting values from HTTP messages is one thing, but we want to ensure that those values are both present and valid. For this purpose, we can use a [Lens](https://www.schoolofhaskell.com/school/to-infinity-and-beyond/pick-of-the-week/basic-lensing).

A Lens is a bi-directional entity which can be used to either **get** or **set** a particular value from/onto an HTTP message. http4k provides a DSL
to configure these lenses to target particular parts of the message, whilst at the same time specifying the requirement for those parts (i.e. mandatory or optional).

To utilise a lens, first you have to declare it with the form `<Location>.<configuration and mapping operations>.<terminator>`.

There is one "location" type for each part of the message, each with config/mapping operations which are specific to that location:

| Location  | Starting type | Applicable to           | Multiplicity         | Requirement terminator | Examples  |
------------|---------------|-------------------------|----------------------|------------------------|------------
| Query     | `String`      | `Request`               | Singular or multiple | Optional or Required   | `Query.optional("name")`<br/>`Query.required("name")`<br/>`Query.int().required("name")`<br/>`Query.localDate().multi.required("name")`<br/>`Query.map(::CustomType, { it.value }).required("name")` |
| Header    | `String`      | `Request` or `Response` | Singular or multiple | Optional or Required   | `Header.optional("name")`<br/>`Header.required("name")`<br/>`Header.int().required("name")`<br/>`Header.localDate().multi.required("name")`<br/>`Header.map(::CustomType, { it.value }).required("name")`|
| Path      | `String`      | `Request`               | Singular | Required   |  `Path.of("name")`<br/>`Path.int().of("name")`<br/>`Path.map(::CustomType, { it.value }).of("name")`|
| FormField | `String`      | `WebForm`               | Singular or multiple | Optional or Required   | `FormField.optional("name")`<br/>`FormField.required("name")`<br/>`FormField.int().required("name")`<br/>`FormField.localDate().multi.required("name")`<br/>`FormField.map(::CustomType, { it.value }).required("name")`|
| Body      | `ByteBuffer`  | `Request` or `Response` | Singular | Required   |  `Body.string(ContentType.TEXT_PLAIN).toLens()`<br/>`Body.json().toLens()`<br/>`Body.webForm(Validator.Strict, FormField.required("name")).toLens()` |

Once the lens is declared, you can use it on a target object to either get or set the value:

- Retrieving a value: use `<lens>.extract(<target>)`, or the more concise invoke form: `<lens>(<target>)`
- Setting a value: use `<lens>.inject(<value>, <target>)`, or the more concise invoke form: `<lens>(<value>, <target>)`

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/core/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/core/example.kt"></script>
