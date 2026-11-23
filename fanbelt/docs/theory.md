# The Fanbelt theory

A working draft. Each rule is numbered so it can be argued with, tested against an example, or struck out.

## The claim

A Fanbelt app is made of five kinds of thing:

- **Facts** are what's stored.
- **States** are every way of reading them.
- **Operations** are the only way to change them.
- **Watchers** keep a browser in line with a state.
- **Jobs** act on something outside the app, such as a payment provider, for one key an operation named.

Everything else, including authorisation, live pages, presence, expiry and background work, is one of these five or a combination of them.

## The five things

**Facts** are durable rows in a tenant's tables. They're the only thing that survives a restart, and the only thing a transaction holds still.

**States** are keyed reads, `Key → Model`, computed from facts, presence and the clock. Each read also leaves a **trace** of what it depended on: the topics it touched and the time it goes stale. Stored, derived, presence and rule are all states. Anything that's read is a state, so it has a name, a key and a trace.

**Operations** take an actor and an input, read states once, decide, write facts, and publish topics after commit. They end in an outcome: changes, rejected, denied or hidden.

**Watchers** hold a state's trace, read the state again when the trace says it may have changed, and act on the whole new value. A region is the watcher that renders for browsers.

**Jobs** do work on one key. An operation names the key, which records it as a fact. The job reads what it needs for that key and acts, with no transaction open, until it returns.

## Rules

1. **Only operations change facts.** Watchers and jobs write by running operations.
2. **The trace is the liveness.** Whoever holds a state's trace knows when its value may have changed, and nothing else is needed to keep a view current. Time is part of the trace: a state that depends on the clock says when it goes stale, and its watchers read it again then.
3. **Publishing means "re-read", never "here's what changed".** Topics carry no data, so watchers act on the whole current value. That makes a missed or duplicate message harmless, and makes retrying the same as publishing again.
4. **An operation drops the trace.** It reads states once, inside its transaction, so it may depend only on facts and its own input. Anything else it relies on must first become a fact. That's why states can read presence and operations can't: presence has a trace, but it isn't a fact.
5. **Authorisation is a state.** A rule is a state whose value is `Allowed(found)`, `Hidden`, `Denied` or `Rejected`. The same rule decides what a page shows and what an operation allows, so the two can't drift apart.
6. **The key is the cost.** Regions share a read per tenant and key, so the key decides how many reads happen. A per-viewer key means a read for each viewer; no key means one read for everyone.
7. **Something has to start the work.** A region starts because a viewer connected. A job has nobody to start it, so the work must announce itself: the operation that makes work names the job's key, which records the key in the same transaction and wakes the job. Every node also runs all recorded keys as it starts and once per heal, so a lost wake makes work late by at most one heal.
8. **Watchers and jobs must settle.** A region settles when nothing it read changes. A job settles when its body returns, which deletes its recorded key. A body that throws runs again a second later, for ever, unless the app records its attempts and stops acting on them. A body can also run after its work is done, so it must then do nothing.
9. **Time is a "not before".** A state that depends on the clock says when it goes stale, as a reservation's expiry does, and its watchers read it again then. A job's retry is the same idea, kept by Fanbelt: a failed key runs again a second later. The retry time isn't stored, so after a restart the key runs at once. There's no backoff, and giving up is up to the app.
10. **Every node is woken; one node acts.** A wake names the job and key, not the tenant: each node finds the tenants owing that key from the recorded rows, so the record is the only source of who owes what. Each run takes a lock on its job, tenant and key without waiting. A node that can't take it leaves the key to the holder, which runs it again if it was named meanwhile. The lock lasts only as long as the run. Operations never take it, so what the body acts on can change while it runs, and the operations it calls decide from current state (rule 1).
11. **A job acts on every run.** A region skips a value equal to the last one it showed, but a job mustn't: a retry reads a value that hasn't changed, so skipping it would drop the retry.
12. **A model is bounded by its key.** What a state reads grows with its key, never with the tenant's history. An operation can only publish keys it knows, so a topic is often coarser than the read that watches it: a window of chat history watches its channel's topic, and reads only the messages it shows.

## On the web

The rules say nothing about HTTP. On the web, they lead to these:

- **The server owns the truth.** The browser shows the HTML it was sent, and posts what the user wants to do.
- **Multiplayer is the default.** Your own change reaches you the same way as everyone else's.
- **Everything you can see has an address.** If the server renders from it, it's in the URL. Changing what you see is following a link.
- **An operation and its route are separate.** The operation is the behaviour, and runs from tests and jobs as well as pages. The route is transport.
- **Nodes are stateless.** Any node can serve any request, because the facts are in the database and "re-read this" goes over a shared bus.

## Where it comes from

- *Out of the Tar Pit* (Moseley and Marks, 2006): essential state becomes facts, derived relations states, feeders operations, and observers watchers. Presence has no place there; here it's a state that isn't a fact (rule 4).
- Incremental computation that records dependencies as it runs (Salsa, Adapton, *Build Systems à la Carte*): a read records its topics, and they decide what's read again.
- Level-triggered reconciliation (Kubernetes controllers): watch, plus a periodic resync, acting on the current value rather than the event.
- Functional reactive programming: states are values over time, and operations are events.

What's less common is pushing authorisation, presence and time through the same mechanism, all the way to server-rendered HTML.

## Tested so far

| Prediction | Where | Result |
|---|---|---|
| A rule is a state, so a page and an operation can share it | sl4k: `visibleChannel` guards the channel pages and every channel operation | Held |
| Operations can't depend on presence | core: a rule an operation uses fails if it reads presence | Held (rule 4) |
| Derive rather than store | sl4k: teammates derived from read marks, and a stored table dropped | Held |
| The key is the cost | sl4k: the sidebar became one read for everyone instead of one per viewer | Held (rule 6) |
| Background work is a watcher on a state | sl4k previews and helpdesk notifications, each watching a state, with a resync to find tenants | Worked, and found rules 7 and 8, but didn't pay: a forgotten publish became a delay rather than a test failure, and every tenant needed a watcher. Replaced by the next row |
| A job on one key, woken by the operation that names it | box office payments, helpdesk notifications, sl4k previews | Held. Work happens at once, a forgotten wake fails the example's tests, and a lock per key replaced each example's own claims, leases and backoff. The cost: retry is fixed at a second, and giving up is each app's (the helpdesk stops after four tries; the box office never does) |

## Edges

- **Effects outside the app happen at least once.** If a node acts and dies before its recorded key is deleted, the key runs again. Only an idempotency key at the far end makes it effectively once. The theory can't remove this; it only makes the window explicit.
- **Finding recorded work is outside the theory.** Recovery reads every tenant's recorded keys with a cross-tenant query, which isn't a state.
- **A key a dead node was running waits.** Its lock goes with the node, but the key runs again only when it's named again, or at the next start or heal.
- **A job doesn't care where it runs.** It needs only storage, the bus and locks, so jobs can run in a worker process of their own, scaled apart from the web nodes.
- **Following new topics costs a second read.** A watcher learns its topics by reading, so it can only subscribe after a read, and a publish in between would be missed. So whenever its topics change, it subscribes and reads again. That's the price of topics carrying no data (rule 3): with no version to compare, reading again is the only way to know nothing was missed.

## Open questions

- Should Fanbelt offer backoff, or a dead letter after so many attempts, rather than leaving both to each app?
- When an operation must name a job in a feature it doesn't build on, should the job's topic be declared apart from both? sl4k turned a dependency around instead.
- Can rule 8 be enforced, for example by a test kit check that a region or job settles?
