# http4k-validation

Composable, message-carrying request validation for http4k, built as an extension of
[values4k](https://github.com/fork-handles/forkhandles/tree/trunk/values4k).

The validation primitives live in the `dev.forkhandles.values` package, alongside values4k's
own types, so they can be moved upstream into values4k without any package renames.

## Install

```kotlin
implementation("org.http4k:http4k-validation")
```

## Concepts

A `Validator<T, Problem>` is a function `(T) -> Problem?`:

- returns `null` when the value is valid,
- returns a `Problem` describing why it is not.

```kotlin
val validator = min(18)

validator(30)   // null
validator(10)   // Problem(constraint = "Min", message = "must be greater than or equal to 18", value = "10")
```

A `Problem` carries the constraint name, a human-readable `message` and the offending `value`:

```kotlin
interface Problem {
    val constraint: String
    val message: String
    val value: String
}
```

## Combining validators

```kotlin
val valid = size(min = 2) and email()          // fails on the first problem
val either = isNull<String>() or notBlank()    // passes if either passes
val all = allOf(notBlank(), size(max = 100))   // aggregates every failure into a Problems
```

## Getting a Result

result4k is an optional dependency, bridged by a small shim:

```kotlin
val result = min(18).resultOf(30)   // Success(30)
val failure = min(18).resultOf(10)  // Failure(Problem(...))
```

## Available validations

| http4k-validation | Jakarta equivalent | Applies to |
|---|---|---|
| `notNull()` | `@NotNull` | any |
| `isNull()` | `@Null` | any |
| `notBlank()` | `@NotBlank` | `CharSequence` |
| `notEmpty()` | `@NotEmpty` | `CharSequence` |
| `size(min, max)` | `@Size(min, max)` | `CharSequence` |
| `email()` | `@Email` | `CharSequence` |
| `pattern(regex)` | `@Pattern(regexp)` | `CharSequence` |
| `min(n)` | `@Min(value)` | numeric |
| `max(n)` | `@Max(value)` | numeric |
| `decimalMin(n)` | `@DecimalMin(value)` | `BigDecimal` |
| `decimalMax(n)` | `@DecimalMax(value)` | `BigDecimal` |
| `positive()` | `@Positive` | numeric |
| `positiveOrZero()` | `@PositiveOrZero` | numeric |
| `negative()` | `@Negative` | numeric |
| `negativeOrZero()` | `@NegativeOrZero` | numeric |
| `digits(integer, fraction)` | `@Digits(integer, fraction)` | `BigDecimal` |
| `isTrue()` | `@AssertTrue` | `Boolean` |
| `isFalse()` | `@AssertFalse` | `Boolean` |
| `past()` | `@Past` | `Instant` |
| `pastOrPresent()` | `@PastOrPresent` | `Instant` |
| `future()` | `@Future` | `Instant` |
| `futureOrPresent()` | `@FutureOrPresent` | `Instant` |
| `notEmptyCollection()` | `@NotEmpty` | `Collection` |
| `sizeCollection(min, max)` | `@Size(min, max)` | `Collection` |

## Extensions to Jakarta

Two features go beyond the Jakarta-style constraints above.

### Lens support

http4k lenses already validate their input: requiredness, parsing and content type. `validated()`
attaches a `Validator` to a lens, so constraint failures flow through the same `Problem` channel:

```kotlin
val email = Query.required("email").validated(email())
val age = Query.int().required("age").validated(min(18))
val id = Path.int().of("id").validated(min(1))
val body = Body.auto<CreateUser>().toLens().validated(...)
```

Lens failures also become `Problem`s: `Missing` (required value absent), `Invalid` (parse failure)
and `Unsupported` (content type not acceptable). Handle everything with one filter:

```kotlin
val app = ServerFilters.CatchValidationFailure().then(handler)
```

### Composition

Compose validators onto an object's fields with the `validated` infix and `allOf`:

```kotlin
Body.auto<CreateUser>().toLens().validated(
    allOf(
        CreateUser::name validated notBlank(),
        CreateUser::email validated email(),
        CreateUser::age validated min(18)
    )
)
```

`allOf` aggregates every failure into a `Problems`.
