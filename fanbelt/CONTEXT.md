# Fanbelt glossary

Fanbelt builds live, multi-user web apps on http4k: the server holds the state, and every open page re-renders what changed. These are its terms. To learn them in order, read [the guide](docs/guide.md); for their API, [the reference](docs/reference.md).

## The model

**Fact**:
A durable row in a tenant's tables. The only thing that survives a restart.

**State**:
A named, keyed read, such as the bookings for one show. Its value may be `null`, meaning its subject doesn't exist.
_Avoid_: query, view model

**Stored state**:
A state read straight from the domain.

**Derived state**:
A state built from other states, read in one transaction. It may also say when it goes stale by the clock.

**Presence**:
A state of who's here: a member joins while their page is open, and may carry one value, such as a cursor. It isn't a fact, so operations can't read it.

**Rule**:
A state that decides whether an actor may do something, answering `Allowed` with what it found, or a refusal. Operations, pages and routes share it.

**Topic**:
The name of a state for one key, such as `bookings:<show>`. Publishing it means "re-read this"; it carries no data.

**Trace**:
What a read depended on: the topics it touched, and when it goes stale.

**Operation**:
The only way to change facts. It takes an actor and an input, runs in one transaction, and ends in an outcome.
_Avoid_: command, action, mutation

**Outcome**:
How an operation ended: `Changes` (accepted, naming the topics to publish after commit), `Rejected` (a reason shown to the actor), `Denied` (403) or `Hidden` (404). Rules decide it on the server, and that is the security boundary: hiding a button protects nothing.

**Verdict**:
What a test saw an action come to: `Accepted`, `Rejected`, `Denied`, `Hidden`, or `Unavailable` when the page offered no way to try. The domain driver gives the outcome exactly; a UI driver only sees what the page shows.

**Watcher**:
Something that holds a state's trace and reads the state again when the trace says it may have changed. A region is one.

**Job**:
Work on one key that runs outside any transaction, such as charging one order. An operation names the key, which records it as a fact; the job is done when its body returns.

## The app's code

**Domain class**:
The app's logic over its tables, built for each transaction. It never checks permissions or publishes.

**Tables object**:
The app's typed handles to its tables, for one tenant.

**Feature**:
One area of the app: its states, rules, operations and jobs, over its domain class. It has no web dependency.

**Actor**:
Who an operation acts as: a person in a role, or the system.
_Avoid_: user (that's who the person is, before a role is applied)

**Viewer**:
The person behind a request.

## The web

**Region**:
A live part of a page that shows one state for one key. It re-renders whole when its trace fires, and is sent only if its HTML changed.
_Avoid_: component, widget

**Page**:
One route that serves a first render of its regions, and then one stream that keeps them live.

**Route**:
An operation, or a rule, bound to an HTTP route.

**One-shot read**:
A route that reads a state once and patches it into the page. It isn't live.

**Signals**:
The browser's small pieces of state, such as a field's text or an error, declared once as a page's signals class.

## Running it

**Tenant**:
An isolated customer space: a theatre, a team, an organisation. Code is always in exactly one, and can't choose another.

**Workspace**:
What finds a request's tenant, from its URL or otherwise.

**Scope**:
One tenant's way in: run an operation, read a state, publish a topic.

**Bus**:
Carries published topics to every watcher. Across nodes, it's a backplane over a transport such as Postgres `LISTEN/NOTIFY` or Redis.

**Heal**:
The periodic re-read, every five minutes by default, that catches anything a lost message missed, including recorded jobs.

**Runtime**:
Everything with side effects: the clock, randomness, the scheduler, events and outbound HTTP.

**Infra**:
What features share: storage, the bus and presence.

**Node**:
One running server: the app's handler, plus the parts with background work, started and stopped together.

**Trace event**:
An event for one step a change takes: an operation executed, a topic published, a region re-read, a browser patched.

**Marble diagram**:
Trace events drawn as one lane per operation, topic, region and browser.

## Testing

**Contract**:
A test's scenarios written once, in the app's language, against a driver. An interface, never a class: one per area of the app, composed into the app's contract, and kept in `testFixtures`.

**Driver**:
One way of running a contract: through the features (a test tenant), through an emulated browser, or through a real one.

**Test tenant**:
One tenant with its features, in memory, on a clock and scheduler the test controls. It fails a step that changes a state it has read without publishing that state's topic.
