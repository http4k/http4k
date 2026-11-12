# The Fanbelt theory

A working draft, to be checked on its own. Each rule is numbered so it can be argued with, tested against an example, or struck out.

## The claim

An Fanbelt app is made of four kinds of thing. **Facts** are what's stored. **States** are every way of reading them. **Operations** are the only way to change them. **Watchers** keep something outside the app (a browser, a remote site, a mail provider) in line with a state. Everything else, including authorisation, live pages, presence, expiry and background work, is one of these four or a combination of them.

## The four things

**Facts**: durable, transactional rows in a tenant's tables. They're the only thing that survives a restart, and the only thing a transaction holds still.

**States**: a keyed read, `Key → Model`, computed from facts, presence and the clock. Each read also leaves a **trace** of what it depended on: the topics it touched, the presence it read, and the time it goes stale (`staleAt`). Stored, derived, presence and rule are all states, and every read is one: if something is read, it has a name, a key and a trace.

**Operations**: take an actor and an input, read states once, decide, write facts, and publish topics after commit. They end in an outcome: changes, rejected, denied or hidden.

**Watchers**: hold a state's trace and read the state again when the trace says it may have changed, then act on the whole new value. A watcher is a role, not a type: today it's filled by regions and by the jobs' sweeps. A region is a watcher that renders for browsers. A background job is a watcher that acts on something remote. A job that polls is still a watcher: one with no topics, whose read goes stale a second later.

## Rules

1. **Only operations change facts.** Watchers, pages and jobs write by running operations.
2. **The trace is the liveness.** Whoever holds a state's trace can know when its value may have changed. Nothing else is needed to keep a view current. Time is part of the trace too: a state that depends on the clock says when it goes stale, and its watchers read it again at that moment.
3. **Publishing means "re-read", never "here's what changed".** Topics carry no payload, so watchers are level-triggered: they act on the whole current value, which makes missed or duplicate messages harmless, and makes retrying the same as publishing again.
4. **An operation drops the trace.** It reads states once, inside its transaction. So an operation may depend only on facts and its own input. Anything else it needs to rely on must first become a fact. This is why presence is visible to states but not to operations: presence is a state with a trace, but it isn't a fact.
5. **Authorisation is a state.** A rule is a state whose model is `Allowed(found)`, `Hidden`, `Denied` or `Unavailable`. The same rule gates what a page shows and what an operation allows, so the two can't drift apart.
6. **The key is the cost.** Regions share a read per tenant and key, so the choice of key decides how many reads happen. A per-viewer key means a read for each viewer; a `Unit` key means one read for everyone.
7. **Something has to start a watcher.** A region starts because a viewer connected (demand). A job has nothing to start it, so it has to find the work (supply). Today each job sweeps every second, asking which tenants have pending rows and acting on each.
8. **A watcher must converge.** It reads its state again after it acts, by a publish or by the next sweep. It settles only if its operations move the work out of the state. Otherwise it does the same work again on every read: endlessly, if it watches topics its own operations publish.
9. **Exclusion over time is a "not before" on a fact.** Backoff after a failure and a claim (lease) by one node are the same thing: the item carries a time before which it isn't due. The state leaves it out until then, and a watcher must read again by then: a sweep does within a second, a watcher with topics needs `staleAt` set to the earliest such time. A claim is an operation, so the transaction picks one winner among nodes, and a dead node's claim expires on its own.
10. **Reads are duplicated across nodes; effects are made once by claims.** Every node sweeps, or is woken by the bus, and each reads the same state. Only the claim decides who acts. A watcher should claim one item, act on it, then claim the next, so each claim only has to cover one action and the other nodes can take the rest. Claiming a whole batch first means a slow send holds every claim, and a claim that expires before its send lets another node send it too.
11. **A watcher is given every read, not only changes.** A view skips a value equal to the last one it showed, but an effect mustn't: retrying after a failure is a re-read of a value that hasn't changed, so dropping unchanged values would drop the retry. Skipping is the region's choice.

## Where it comes from

- *Out of the Tar Pit* (Moseley & Marks, 2006), Functional Relational Programming: essential state → facts, derived relations → states, feeders → operations, observers → watchers. Presence has no place there; here it's a state that isn't a fact (rule 4).
- Incremental computation with dependencies recorded while it runs (Salsa, Adapton, *Build Systems à la Carte*): a read records its topics, and the record decides what is read again.
- Level-triggered reconciliation (Kubernetes controllers): watch plus periodic resync, acting on the current value rather than on the event.
- Functional reactive programming: states are behaviours (values over time, with `staleAt` making time explicit), and operations are events.

What's less common is pushing authorisation, presence and time through the same mechanism, all the way to server-rendered HTML.

## Tested so far

| Prediction | Where | Result |
|---|---|---|
| A rule is a state, so a page and an operation share it | sl4k `visibleChannel`: the gate region and every channel operation | Held |
| Operations can't depend on presence | core: an operation's read context has no presence, and a rule that reads it fails loudly | Held (rule 4) |
| Derive instead of store | sl4k teammates derived from read marks, a stored table dropped | Held |
| Key choice is cost | sl4k sidebar: one read for everyone instead of one per viewer | Held (rule 6) |
| Background work is a watcher on a state | sl4k preview job and helpdesk notification job, on the pending-previews-state branch: each job watched a state's topics and its `staleAt`, with a 30 second resync to enlist tenants | Held. Found rules 7 and 8 |
| Retry is reading again | sl4k: a failed fetch stays pending and is tried again at the next sweep | Held (rule 3) |
| Backoff and claims are one mechanism | helpdesk outbox: a claimed or backing-off item carries a `dueAt` and isn't due until then | Held (rule 9) |
| Claim one item, act, then claim the next | helpdesk: while one email is sent, the rest stay due for another node; claiming the whole batch first failed this | Held (rule 10) |
| Watching beats polling for jobs | the same branch, against main's one-second sweeps | Worked, didn't pay. Work happened at once rather than within a second, but every write had to publish the right topic, a forgotten publish became a 30 second delay rather than a failure, a watcher was kept for every tenant rather than only those with work, and core gained a watch primitive whose threading caused two hangs. The sweeps stayed |

## Edges

- **External effects are at least once.** If a node performs an effect and dies before the operation recording it commits, the claim expires and the effect happens again. Only an idempotency key at the remote end makes it effectively once. The theory can't remove this; it only makes the window explicit.
- **Finding tenants is outside the theory.** A sweep needs the cross-tenant `having` query, which isn't a state.
- **A watcher doesn't care where it runs.** It needs only the trace, the bus and storage, so background watchers can run in their own worker process, sharing infrastructure with the web nodes but scaled and deployed separately. That needs a bus that crosses processes.
- **Following new topics costs a second read.** A watcher learns its topics by reading, so it can only subscribe after a read, and a publish in between would be lost. So whenever its topics change, including when it starts, it subscribes and reads again. That's the price of publishes carrying nothing (rule 3): without a version to compare, reading again is the only way to know nothing was missed.

## Open questions

- If duplicated reads across nodes ever cost too much, the next step is rule 9 at tenant level: one node leases the watching of a tenant. Item claims stay, to be safe while a lease changes hands.
- Is a sweep the right general answer to rule 7, or should storage offer a way to watch for tenants with work across all tenants? Watching per tenant was tried and didn't pay.
- Is rule 8 enforceable, for example by a test kit check that a watcher settles?
