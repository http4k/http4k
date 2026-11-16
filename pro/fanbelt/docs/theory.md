# The Fanbelt theory

A working draft, to be checked on its own. Each rule is numbered so it can be argued with, tested against an example, or struck out.

## The claim

A Fanbelt app is made of five kinds of thing. **Facts** are what's stored. **States** are every way of reading them. **Operations** are the only way to change them. **Watchers** keep a browser in line with a state. **Jobs** act on something remote (a payment provider, a mail provider, another site) for one key an operation named. Everything else, including authorisation, live pages, presence, expiry and background work, is one of these five or a combination of them.

## The five things

**Facts**: durable, transactional rows in a tenant's tables. They're the only thing that survives a restart, and the only thing a transaction holds still.

**States**: a keyed read, `Key → Model`, computed from facts, presence and the clock. Each read also leaves a **trace** of what it depended on: the topics it touched, the presence it read, and the time it goes stale (`ctx.stale(at = …)`). Stored, derived, presence and rule are all states, and every read is one: if something is read, it has a name, a key and a trace.

**Operations**: take an actor and an input, read states once, decide, write facts, and publish topics after commit. They end in an outcome: changes, rejected, denied or hidden.

**Watchers**: hold a state's trace and read the state again when the trace says it may have changed, then act on the whole new value. A region is the watcher that renders for browsers.

**Jobs**: work on one key of a stored state. A job doesn't follow a trace: an operation names its key, which records it as a fact, and the job reads that key's model and acts, with no transaction open, until it returns.

## Rules

1. **Only operations change facts.** Watchers and jobs write by running operations.
2. **The trace is the liveness.** Whoever holds a state's trace can know when its value may have changed. Nothing else is needed to keep a view current. Time is part of the trace too: a state that depends on the clock says when it goes stale, and its watchers read it again at that moment.
3. **Publishing means "re-read", never "here's what changed".** Topics carry no payload, so watchers and jobs are level-triggered: they act on the whole current value, which makes missed or duplicate messages harmless, and makes retrying the same as publishing again.
4. **An operation drops the trace.** It reads states once, inside its transaction. So an operation may depend only on facts and its own input. Anything else it needs to rely on must first become a fact. This is why presence is visible to states but not to operations: presence is a state with a trace, but it isn't a fact.
5. **Authorisation is a state.** A rule is a state whose model is `Allowed(found)`, `Hidden`, `Denied` or `Rejected`. The same rule gates what a page shows and what an operation allows, so the two can't drift apart.
6. **The key is the cost.** Regions share a read per tenant and key, so the choice of key decides how many reads happen. A per-viewer key means a read for each viewer; a `Unit` key means one read for everyone.
7. **Something has to start the work.** A region starts because a viewer connected (demand). A job has nothing to start it, so the work has to find it (supply). The operation that makes work names the job's key, which records the key as a fact in the same transaction and wakes the job. As a node starts, or its backplane reconnects, it runs every recorded key, so a lost wake only makes work late.
8. **Watchers and jobs must converge.** A region settles when nothing it read changes. A job settles when its body returns, which deletes its recorded key. A body that throws runs again a second later, for ever, unless the app records its attempts as facts and the body stops acting on them. A body can also run after its work is done, so it must do nothing then.
9. **Time is a "not before" on a fact.** A state that depends on the clock says when it goes stale (`ctx.stale(at = …)`), as a reservation's expiry does, and its watchers read it again then. A job's retry is the same thing kept by Fanbelt: a failed run leaves its recorded key with a time a second later, so it runs again then, after a restart too. There's no backoff, and giving up is the app's, in its own facts.
10. **Wakes reach every node; one node acts, by a lock.** Every node running jobs is woken by the bus. Each run takes a lock on its job, tenant and key without waiting, and a node that can't take it tries again a second later, in case the work changed after that run began. The lock lasts only as long as the run, so a dead node's lock goes with its connection.
11. **A job is given every run, not only changes.** A region skips a value equal to the last one it showed, but a job mustn't: retrying after a failure reads a value that hasn't changed, so skipping unchanged values would drop the retry.

## On the web

The rules above say nothing about HTTP. These say how a web app built on them behaves:

- **The server owns the truth.** It's in the database. The browser shows HTML it was sent and posts what the user wants to do.
- **Multiplayer is the default.** Your own change reaches you the same way as everyone else's: a region re-renders for everyone watching, the person who clicked included.
- **Everything you can see has an address.** If the server renders from it, it's in the URL. Changing what you see is following a link.
- **An operation and its route are declared separately.** The operation is the behaviour, and runs from tests and background work as well as pages (rule 1). The route is transport.
- **Nodes are stateless.** Any node can serve any request, because the facts are in the database and "re-read this" is on a shared bus.

## Where it comes from

- *Out of the Tar Pit* (Moseley & Marks, 2006), Functional Relational Programming: essential state → facts, derived relations → states, feeders → operations, observers → watchers. Presence has no place there; here it's a state that isn't a fact (rule 4).
- Incremental computation with dependencies recorded while it runs (Salsa, Adapton, *Build Systems à la Carte*): a read records its topics, and the record decides what is read again.
- Level-triggered reconciliation (Kubernetes controllers): watch plus periodic resync, acting on the current value rather than on the event.
- Functional reactive programming: states are behaviours (values over time, with `ctx.stale` making time explicit), and operations are events.

What's less common is pushing authorisation, presence and time through the same mechanism, all the way to server-rendered HTML.

## Tested so far

| Prediction | Where | Result |
|---|---|---|
| A rule is a state, so a page and an operation share it | sl4k `visibleChannel`: the channel pages and every channel operation | Held |
| Operations can't depend on presence | core: an operation's read context has no presence, and a rule that reads it fails loudly | Held (rule 4) |
| Derive instead of store | sl4k teammates derived from read marks, a stored table dropped | Held |
| Key choice is cost | sl4k sidebar: one read for everyone instead of one per viewer | Held (rule 6) |
| Background work is a watcher on a state | sl4k preview job and helpdesk notification job, on the pending-previews-state branch: each job watched a state's topics and its `staleAt`, with a 30 second resync to enlist tenants | Held. Found rules 7 and 8 |
| Retry is reading again | sl4k: a failed fetch stays pending, and its read goes stale a second later | Held (rule 3). Now the job's own: a failed run stays recorded and runs again a second later |
| Backoff and claims are one mechanism | helpdesk outbox: a claimed or backing-off item carries a `dueAt` and isn't due until then | Held, then dropped. Claims became locks and backoff became a fixed retry, both in Fanbelt (rows below) |
| Claim one item, act, then claim the next | helpdesk: while one email is sent, the rest stay due for another node; claiming the whole batch first failed this | Held, then built in: a job runs one key at a time, under that key's lock |
| Watching beats polling for jobs | the same branch, against main's one-second sweeps | Worked, didn't pay. Work happened at once rather than within a second, but every write had to publish the right topic, a forgotten publish became a 30 second delay rather than a failure, a watcher was kept for every tenant rather than only those with work, and core gained a watch primitive whose threading caused two hangs. The sweeps stayed |
| Waking beats polling, with one wake per job across tenants | helpdesk notifications, sl4k previews, box office payments, replacing each one's sweep | Held. Work happens at once; timers run only in tenants with work waiting; a forgotten wake fails each example's contract (checked by removing one); the runner reuses the bus's coalescing, with no new threading. Costs: a job its own operations wake needed its type stated, and the feature whose operations make the work must be able to see the job, which turned sl4k's dependency around |
| A job on one key beats a job on a whole state | box office payments, helpdesk notifications, sl4k previews, replacing each one's whole-state job | Held. Naming a key wakes the job and records the key in the same transaction, so recovery reads the recorded keys rather than a cross-tenant query. A lock per key replaced each example's claim, lease and backoff fields, and `waiting`, `ctx.stale()` with no time and the stated job type went. Cost: retry is fixed at a second, with no backoff, and giving up moved into each app's domain (the helpdesk stops after four; the box office retries for ever) |

## Edges

- **External effects are at least once.** If a node performs an effect and dies before its recorded key is deleted, the key runs again. Only an idempotency key at the remote end makes it effectively once. The theory can't remove this; it only makes the window explicit.
- **Finding recorded work is outside the theory.** Recovery reads every tenant's recorded keys with the cross-tenant `having` query, which isn't a state.
- **A key a dead node was running waits.** Its lock goes with the node, but its key runs again only when another node was woken for it, or a node starts or reconnects, or it's named again.
- **A job doesn't care where it runs.** It needs only the bus, storage and locks, so jobs can run in a worker process of their own, sharing storage and the bus with web nodes that run none, and scaled separately.
- **Following new topics costs a second read.** A watcher learns its topics by reading, so it can only subscribe after a read, and a publish in between would be lost. So whenever its topics change, including when it starts, it subscribes and reads again. That's the price of publishes carrying nothing (rule 3): without a version to compare, reading again is the only way to know nothing was missed.

## Open questions

- Should Fanbelt offer backoff, or a dead letter after so many attempts, rather than leaving both to each app?
- When one operation must name jobs in features that don't build on its own, should a job's topic be declared apart from both? sl4k turned a dependency around instead.
- Is rule 8 enforceable, for example by a test kit check that a region or job settles?
