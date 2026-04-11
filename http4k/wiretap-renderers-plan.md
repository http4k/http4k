# Wiretap Renderer Plan

Reimplement tracer-bullet renderers inside Intercept, leveraging the richer OTel data model to go beyond what tracer-bullet could do.

## Data Advantage

Tracer-bullet operates on a simple `TraceStep`/`Trace` model: actors, request strings, children. No timing, no attributes, no events.

Wiretap sits on full OTel spans: attributes, baggage, events, links, timing percentages, semantic conventions (`http.method`, `http.status_code`, `db.system`, etc.), plus captured HTTP transactions and structured logs. Every renderer below should exploit this.

---

## Tier 1: Straight Ports

Mechanical reimplementations. Necessary but not differentiating.

### 1.1 D2 Sequence Diagram
Port of `D2SequenceDiagram`. Map OTel spans to D2 sequence syntax. Enrich with HTTP method/status from semantic conventions on each arrow.

### 1.2 D2 Interaction Diagram
Port of `D2InteractionDiagram`. Auto-classify actor shapes from OTel attributes — `db.system=postgresql` → cylinder, `messaging.system=rabbitmq` → queue, etc. Richer than tracer-bullet's manual Actor typing.

### 1.3 PlantUML Sequence Diagram
Port of `PumlSequenceDiagram`. Keep the status-code color coding (2xx green, 3xx blue, 4xx orange, 5xx red). Add timing annotations on arrows.

### 1.4 PlantUML Interaction Diagram
Port of `PumlInteractionDiagram`. C4 container-based. Use OTel resource attributes for richer container metadata.

### 1.5 PlantUML Interaction Flow Diagram
Port of `PumlInteractionFlowDiagram`. Numbered request/response flows. Add cumulative timing alongside flow numbers.

### 1.6 Markdown Depth Table
Port of `MarkdownTraceDepthTable`. Table of max trace depth per origin→target pair, sorted by depth.

### 1.7 Markdown Step Count Table
Port of `MarkdownTraceStepCountsTable`. Table of trace step counts per origin→target pair, sorted by count.

---

## Tier 2: Latency-Enriched Renderers

These take the Tier 1 ports and add timing data that tracer-bullet never had.

### 2.1 Latency Heatmap Sequence Diagram
Any diagram format (Mermaid, D2, PlantUML). Color-code spans by duration — red for slow, green for fast. Annotate edges with milliseconds. Makes slow calls jump off the page.

### 2.2 Latency-Enriched Markdown Tables
Extend the depth and step count tables with p50/p95/max latency per edge. A depth of 8 is fine if it's 2ms. A depth of 3 is a problem if it's 800ms. The table should show both.

### 2.3 Critical Path Renderer
Walk the span tree, find the longest wall-clock path from root to leaf (accounting for parallel spans). Render a sequence diagram where the critical path is highlighted and everything else is dimmed/greyed. Answers: "what's actually making this test slow?"

---

## Tier 3: Novel Renderers

Things tracer-bullet couldn't do at all.

### 3.1 Trace Diff Renderer
Compare two traces — before/after a refactor, between test runs. Render a sequence diagram showing what changed: new calls in green, removed calls in red, changed latency annotated with delta. Markdown tables become diff tables with +/- rows.

Primary use case: PR reviews. "This refactor added an extra database call" becomes visible without reading code.

### 3.2 Error Trace Isolation
Filter the span tree to only the path leading to an error span. Render a focused sequence diagram showing just the failure chain with error event details annotated inline. Pair with the JUnit `OnFailure` render mode — when a test fails, you get the signal, not the noise.

### 3.3 Span Event Timeline
OTel span events (exceptions, retries, state transitions) have timestamps but aren't visualized today. Render as a Mermaid Gantt or markdown table showing events *within* spans. Sub-span visibility.

### 3.4 Weighted Dependency Graph
Interaction diagram with edges weighted by call count and total time. Thick arrows = many calls or high latency. Surfaces "service A calls service B 47 times in this test" — a code smell that standard interaction diagrams hide by deduplicating.

### 3.5 Attribute-Aware Topology
Auto-classify actors from OTel semantic conventions with full precision: not just "Database" but "PostgreSQL", "Redis", "RabbitMQ". Render with format-appropriate shapes. D2 is strongest here with its shape variety.

---

## Tier 4: Composite Output

### 4.1 Living Test Document
Composite markdown renderer (port of `MarkdownDocument`) that embeds everything: depth table, step count table, latency stats, sequence diagram, interaction diagram, and error summary into a single document per test. This is the "test specification" artifact — human-readable documentation generated from actual test execution.

---

## Priority Recommendation

| Renderer | Effort | Value | Do When |
|---|---|---|---|
| Straight ports (1.1–1.7) | Low | Medium | First — foundation for everything else |
| Latency heatmap (2.1) | Medium | High | Second — biggest visual bang for the buck |
| Error isolation (3.2) | Low | High | Second — small effort, huge payoff on failure |
| Critical path (2.3) | Medium | High | Third — the "why is this slow" answer |
| Trace diff (3.1) | High | Very High | Third — hardest to build but most differentiated |
| Weighted dependency (3.4) | Medium | Medium | Fourth |
| Living test doc (4.1) | Low | High | Fourth — glue layer, easy once others exist |
| Span event timeline (3.3) | Low | Medium | Whenever |
| Attribute topology (3.5) | Low | Medium | Fold into Tier 1 ports as you build them |
| Latency tables (2.2) | Low | Medium | Fold into Tier 1 table ports |
