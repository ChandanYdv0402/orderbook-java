# Order Book & Matching Engine — Detailed Plan

A single-instrument limit order book and matching engine in Java 21. It replays real NASDAQ ITCH 5.0 market data and publishes measured latency numbers.

- **Budget:** about 8 hours a week alongside work, for roughly 12 weeks (~96 hours)
- **Real finish line:** Phases 0–4 plus the write-up (about week 9–10)
- **Stretch goals:** Phase 5 (threaded pipeline) and Phase 7 (C++ port)

---

## 1. Definition of done

v1 is done when all four of these are true:

| # | Criterion | How it's proven |
|---|---|---|
| 1 | **Correct** | Price-time priority matching for limit, market, cancel and modify orders. It passes a test suite that compares it against a slow, obviously correct reference engine. |
| 2 | **Fast** | Zero allocations on the hot path after warmup, proven with an Epsilon GC (no-op GC) run. Published p50 / p99 / p99.9 latency per message. |
| 3 | **Real** | Parses a full day of NASDAQ TotalView-ITCH 5.0 sample data and rebuilds the book correctly. |
| 4 | **Explained** | A README with an architecture diagram, the benchmark method, a flame graph, and a results log showing what each optimization bought. |

### Out of scope for v1
Networking, multiple instruments, persistence, risk checks and a UI. Each one is a distraction until the core is fast and correct.

---

## 2. Ground rules

1. **Gates:** don't start a phase until the previous phase's gate passes. If a phase runs long, cut scope inside it. Never skip its gate.
2. **Correct before fast:** no optimization until the naive version is correct and tested.
3. **Keep v1 forever:** it's the reference oracle for every later version.
4. **One change at a time:** each optimization gets its own results-log row.
5. **Determinism:** the same input sequence always produces the same output. Every bug must be replayable from a file.
6. **Prices are `long` ticks, never `double`.**

### What to vibecode and what to hand-write

The project's value is being able to defend every number in an interview. If it shows up in the results table or in an interview question, write it yourself (or rewrite the AI's version line by line until you could rebuild it on a whiteboard).

| Vibecode freely | Hand-write (or rewrite line by line) |
|---|---|
| Gradle, JUnit and jqwik project setup | `SPEC.md` and the first 5 matching tests |
| ITCH decoders (mechanical, driven by the spec) | `checkInvariants()` (you define the rules) |
| JMH and HdrHistogram harness boilerplate | The v2 book: array levels, intrusive lists, best-price cache |
| Golden-file tooling, CLI, file loading | The matching loop |
| README formatting, diagrams | The primitive id map and the order pool |
| The v1 reference engine (only **after** you've written the spec and tests) | The SPSC ring buffer |
| | Reading flame graphs and writing the explanation for each results-log row |

**Risks specific to vibecoding, and how to guard against them:**
- **The oracle breaks.** If the same AI writes v1 and v2 from the same misunderstanding (for example, the modify rule), the property tests pass and both are wrong. Guard: write the spec and the hand-written tests yourself, first.
- **Benchmarks that lie.** Watch for a missing `Blackhole`, no warmup, averages instead of percentiles, and coordinated omission. Review every benchmark line by line.
- **Hidden allocations.** Lambdas, iterators, boxing and `String.format` creep in. Epsilon GC and async-profiler's alloc mode will catch them; you need to understand why.

---

## 3. Machine and environment

- **macOS (your laptop) is fine for Phases 0–4.**
- **Linux is needed for the published numbers and for Phase 5**, because:
  - `perf stat` only exists on Linux
  - OpenHFT thread affinity doesn't really pin threads on macOS
  - you can't disable CPU frequency scaling on Apple Silicon
- Option: a cheap dedicated or bare-metal Linux cloud instance (not a shared vCPU). Write the CPU model in the README.
- **Benchmark hygiene:** use the same machine for every run, note the CPU model, close everything else, repeat each run 3 times, and turn off frequency scaling where possible.

---

## 4. Toolchain

| Tool | What it's for |
|---|---|
| Java 21 + Gradle | Language and build |
| JUnit 5 | Unit tests for matching rules |
| jqwik | Property-based tests: random order streams checked against the reference engine |
| JMH | Microbenchmarks that avoid JIT and dead-code traps |
| HdrHistogram | Recording latency percentiles without losing the tail |
| async-profiler | CPU and allocation flame graphs |
| Agrona (optional) | Primitive maps, off-heap buffers, ring buffers |
| OpenHFT Java-Thread-Affinity | Pinning threads to cores (Phase 5, Linux) |
| LMAX Disruptor | A production ring buffer to compare against yours (Phase 5) |
| IntelliJ | Conditional breakpoints on a specific order id |

### JVM flags to know
| Flag | Purpose |
|---|---|
| `-Xms` = `-Xmx` | Fixed heap size, so the heap never resizes |
| `-XX:+AlwaysPreTouch` | Touch heap pages at startup, not during the run |
| `-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC` | No-op GC: proves zero allocation |
| `-XX:+UseZGC` | Low-pause GC to compare against G1 |
| `-XX:+PrintCompilation` | Shows when the matching loop gets JIT-compiled |

---

## 5. What to learn, and when

You already know JVM tuning, so GC basics are left out. **Learn properly** = interview material. **Skim** = learn enough to use it, or let the AI handle the details.

| When | Topic | Depth |
|---|---|---|
| Week 1 | Market microstructure: bid, ask, spread, levels, tick size, price-time priority, order types, partial fills, L1/L2/L3 | Learn properly |
| Week 1 | Java 21: records, sealed interfaces, pattern-matching `switch` | Skim |
| Week 1 | Gradle (Kotlin DSL) | Skim |
| Week 1 | JUnit 5; jqwik generators, shrinking, stateful testing | Learn jqwik properly |
| Weeks 4–5 | ITCH 5.0 spec: framing and the message layouts | Skim |
| Weeks 4–5 | `MappedByteBuffer`, `ByteBuffer`, byte order, absolute gets; flyweight pattern | Learn properly |
| Weeks 4–5 | Golden-file / snapshot testing | Skim |
| Week 6 | JMH: `@Fork`, `@Warmup`, `@Measurement`, `@State`, `Blackhole`; the JMH samples | Learn properly |
| Week 6 | HdrHistogram API | Skim |
| Week 6 | Coordinated omission | Learn properly |
| Week 6 | async-profiler: CPU and alloc modes, reading flame graphs | Learn properly |
| Weeks 7–9 | CPU caches: 64-byte lines, ~100 ns main-memory miss, prefetching | Learn properly |
| Weeks 7–9 | Intrusive lists, open-addressing hash maps, object pools | Learn properly |
| Weeks 7–9 | Escape analysis; JIT tiers C1/C2; optional JITWatch or `-XX:+PrintInlining` | Learn properly |
| Weeks 10–11 | Java Memory Model: happens-before, `volatile`, `VarHandle` acquire/release | Learn properly |
| Weeks 10–11 | SPSC ring buffer design, false sharing and padding, busy-spin vs parking, `Thread.onSpinWait()` | Learn properly |
| Weeks 10–11 | LMAX Disruptor; thread affinity; Linux `perf stat`, isolated cores, CPU governor | Skim the Linux tooling |
| Stretch | C++17/20 (RAII, move semantics, `std::vector`, custom allocators), CMake, Google Benchmark, ASan/UBSan | As needed |

---

## 6. Concepts 1: how markets match orders

This is the domain layer. Get it straight on paper before writing code, because every later bug traces back to a misunderstood rule.

| Concept | What it means | Where it shows up in the code |
|---|---|---|
| Order | Side (buy/sell), price, quantity, id, timestamp | The core `Order` type |
| Bid / ask, spread | Best buy price, best sell price, and the gap between them | Best-price tracking on each side of the book |
| Price level | All resting orders at one price, queued in arrival order | One `Level` object per price |
| Tick size | Smallest price step; prices are whole numbers of ticks | Store prices as `long` ticks, never `double` |
| Price-time priority | Best price fills first; at equal price, the earliest order fills first | The core matching loop |
| Aggressive vs passive | An order that crosses the spread takes liquidity; one that rests adds it | Decides whether to match or insert |
| Partial fill | An order fills against several resting orders, or only partly | Loop until filled or no more crossing prices |
| Limit / market / IOC | Limit rests its leftover qty; market and IOC cancel the leftover | Order-type branch after matching |
| Cancel / modify | Remove an order, or change its qty or price | Needs O(1) lookup by order id |
| Events out | Accepted, trade, cancelled, book-changed | The output your tests assert on |
| L1 / L2 / L3 data | Top of book / total per price / every individual order | ITCH is L3, the most detailed |
| Determinism | The same input sequence always gives the same output | Makes every bug replayable |

### Modify rule (decide it in SPEC.md)
On most exchanges, **increasing qty or changing price loses time priority**; **reducing qty keeps it**.

### Matching engine vs book builder
NASDAQ's ITCH feed is the exchange's *output*: the matching already happened there. Replaying ITCH means building a **book builder** (apply adds, executes and cancels to a book), not a matching engine. You build both:
1. **Matching engine:** takes your own order stream (synthetic, or derived from ITCH) and decides the trades.
2. **Book builder:** consumes ITCH and reconstructs the exchange's book.

Both share the same book data structure, which is the part you'll optimize hardest.

### ITCH 5.0 messages you need
| Code | Message |
|---|---|
| `A` | Add order (no MPID attribution) |
| `F` | Add order (with MPID attribution) |
| `E` | Order executed |
| `C` | Order executed with price |
| `X` | Order cancel (partial) |
| `D` | Order delete |
| `U` | Order replace |
| `R` | Stock directory (maps stock locate codes to tickers) |

All fields are big-endian. Check the spec for the length prefix used in framing.

---

## 7. Concepts 2: data structures for the book

The book needs three fast operations: find the best price, add or remove at a price, and cancel any order by id. Build the simple version first, then replace each piece and measure the difference.

| Piece | v1 (correct and simple) | v2 (fast) | Why v2 wins |
|---|---|---|---|
| Price levels | `TreeMap<Long, Level>` per side | Array indexed by price tick, within a band around the mid | O(1) access, contiguous memory, no tree nodes allocated |
| Orders in a level | `ArrayDeque<Order>` | Intrusive doubly-linked list (prev/next fields on `Order` itself) | Cancel from the middle in O(1), no wrapper nodes |
| Order lookup by id | `HashMap<Long, Order>` | Primitive open-addressing map (Agrona `Long2ObjectHashMap` or your own) | No boxing of `Long` keys, fewer cache misses |
| Order objects | `new Order(...)` per message | Pre-allocated object pool, reused | Zero garbage, so no GC pauses |
| Best bid / ask | `firstKey()` / `lastKey()` | Cached index, updated on insert and when a level empties | One field read instead of a tree walk |

**The price band edge case:** the array design breaks for a price outside the band. Handle it with a slow-path overflow structure, and **count how often it fires**. On real ITCH data it should be rare. (This is a standard interview question.)

**The oracle:** v1 is kept forever. Property tests feed the same random order stream to v1 and v2 and assert identical events out. This is what lets you optimize aggressively without breaking correctness.

---

## 8. Concepts 3: low-latency JVM techniques

In low-latency Java, the tail (p99.9) matters more than the average. The tail is mostly GC pauses, cache misses and JIT surprises; each technique below targets one of those.

### Allocation and GC
- Every `new` on the hot path is future GC work. Pool objects, use primitive collections, and avoid boxing, iterators, lambdas and string building inside the matching loop.
- Escape analysis can remove some allocations, but don't rely on it. Verify with async-profiler's allocation mode.
- Run the benchmark under Epsilon GC. If the run survives with a small heap, you've proven zero allocation.
- For normal runs, compare G1 against ZGC and note the tail difference.

### JIT warmup
- The JVM interprets first, then compiles hot code through the C1 and C2 tiers. Measurements before warmup are meaningless.
- Warm up with a few hundred thousand messages before recording.
- Run with `-XX:+PrintCompilation` once to see when the matching loop gets compiled.

### Memory layout and caches
- A cache line is 64 bytes. A main-memory miss costs on the order of 100 ns, which can dwarf all your matching logic.
- Arrays of primitives beat graphs of objects: a level array walks memory in order, while a tree hops between scattered nodes.
- False sharing: two threads writing fields on the same cache line slow each other down. Pad hot fields once you go multi-threaded.

### Threading model
- Keep matching single-threaded (the LMAX approach): one thread owns the book, so there are no locks.
- Add concurrency only at the edges. A parser thread hands messages to the matcher through a single-producer single-consumer (SPSC) ring buffer.
- Busy-spin instead of parking for the lowest latency, and pin threads to cores for stable numbers.

### Zero-copy parsing
- Read ITCH from a memory-mapped file with `MappedByteBuffer` or Agrona's `UnsafeBuffer`.
- Flyweight pattern: one reusable decoder object reads fields straight from the buffer at an offset, instead of creating a message object per record.

---

## 9. Concepts 4: measuring it honestly

The numbers are the product. A wrong benchmark is worse than none, because an interviewer will spot it. Measure at two levels.

### Microbenchmarks with JMH
One operation in isolation: add, cancel, match one level.
- Use `@Fork`, `@Warmup` and `@Measurement`.
- Pass results to a `Blackhole` so the JIT can't delete your code as unused.
- Read the official JMH samples once. They're a crash course in every way benchmarks lie.

### End-to-end latency
One message in, its events out.
- Time each message with `System.nanoTime()` and record it into an `HdrHistogram`.
- Report p50, p90, p99, p99.9 and max. **Never just the average.**
- Understand coordinated omission: if the load generator waits for slow responses, it silently skips the worst cases (Gil Tene, "How NOT to Measure Latency").
- Report throughput (messages per second) alongside latency.

### Profiling
- async-profiler CPU mode: where time goes.
- async-profiler alloc mode: who still allocates.
- On Linux, `perf stat` shows cache misses and branch mispredictions, which explain *why* one version is faster.

### Results log
Fill in one row per change. This table is the centrepiece of the README.

| Version | Change | p50 (ns) | p99 (ns) | p99.9 (ns) | Throughput (msg/s) | Alloc / msg (bytes) |
|---|---|---|---|---|---|---|
| v1.0 | Baseline: TreeMap + HashMap | | | | | |
| v2.1 | Array-indexed levels + cached best price | | | | | |
| v2.2 | Intrusive lists in levels | | | | | |
| v2.3 | Primitive id map | | | | | |
| v2.4 | Order object pool | | | | | |
| v2.5 | Flyweight decoder | | | | | |
| v3.0 | Threaded: own SPSC ring buffer | | | | | |
| v3.1 | Threaded: LMAX Disruptor | | | | | |

---

## 10. Architecture

```
 ITCH file (mmap)            Synthetic order stream
        │                              │
        ▼                              │
 Flyweight decoder                     │
 (parser thread)                       │
        │                              │
        ▼                              │
 SPSC ring buffer  ◄───────────────────┘
        │
        ▼
 Matching thread (owns everything, no locks)
   ├── Matching engine  (your own orders → trades)
   ├── Book builder     (ITCH → reconstructed book)
   └── Shared book: bid/ask level arrays, intrusive lists,
                    id map, order pool, overflow slow path
        │
        ▼
 Events out: accepted / trade / cancelled / book-changed
        │
        ├── HdrHistogram (latency per message)
        └── Tests (event-stream comparison)

 Reference engine (v1): runs ONLY in tests, never in benchmarks.
```

- The parser runs on its own thread and hands messages across the ring buffer.
- The matching thread owns every book structure, so nothing on the hot path takes a lock.
- The reference engine exists only so tests can compare event streams.

---

## 11. Phases

### Phase 0: Learn and set up (week 1)

**Reading**
- WK Selph, "How to Build a Fast Limit Order Book"
- Martin Fowler, "The LMAX Architecture"
- NASDAQ TotalView-ITCH 5.0 spec: skim the layouts of `A F E C X D U R`
- Harris, *Trading and Exchanges*: chapters on order types and matching

**Day-one checklist (about 4 hours).** The goal is a repo with a written spec and failing tests, not working code.
- [ ] Create a public repo (e.g. `orderbook-java`) with Java 21, Gradle, JUnit 5 and jqwik.
- [ ] Write `SPEC.md` in half a page: the order types you support, the events you emit, the exact priority rule, and the modify rule.
- [ ] Define the core types: `Side` enum; `Order` (id, side, `priceTicks` as `long`, `qty` as `long`, timestamp); an `Event` type for accepted / trade / cancelled.
- [ ] Write 5 matching tests before any engine code. For example:
  - buy 100 @ 10 rests
  - then sell 50 @ 10 fills 50 and leaves 50 resting
  - a market sell into an empty book is cancelled
  - (plus 2 of your own, e.g. a time-priority test at one price, and a sweep across several levels)
- [ ] Start downloading one NASDAQ TotalView-ITCH 5.0 sample file (search "NASDAQ ITCH 5.0 sample file"). Files are several GB, so start early.
- [ ] Skim the ITCH 5.0 spec for the `A F E C X D U R` layouts.

If you only finish the first four boxes, that's still a good start.

**Gate:** the repo builds, `SPEC.md` exists, and 5 tests fail for the right reason.

---

### Phase 1: Correct naive engine (weeks 2–3)
- [ ] TreeMap-based book with limit and market orders, and partial fills
- [ ] IOC orders (cancel the leftover)
- [ ] Cancel and modify, following the rule in `SPEC.md`
- [ ] `checkInvariants()` called after every event in tests:
  - best bid < best ask
  - each level's total equals the sum of its orders
  - no empty levels left in the book
  - every order in the id map is in the book (and vice versa)
- [ ] jqwik property test: 1M random orders, invariants always hold
- [ ] Deterministic replay: log every input message to a file that can be replayed

**Gate:** all hand-written tests are green, and the 1M-order property test passes.

---

### Phase 2: ITCH parser and book builder (weeks 4–5)
- [ ] Memory-map the file and handle message framing (check the spec for the length prefix)
- [ ] Decoders for `A F E C X D U R` (all fields big-endian)
- [ ] Use `R` messages to map stock locate codes to tickers; filter to one ticker
- [ ] Book builder for one ticker; print the top 5 levels at market close
- [ ] Golden-file test on the first 100k messages: save the resulting book as a text file and compare on every run
- [ ] Sanity check against reality: compare the reconstructed closing state (prices and volume) with publicly known data for that ticker and day

**Gate:** a full-day replay finishes, the golden-file test is green, and the closing book looks plausible.

---

### Phase 3: Measurement harness (week 6)
- [ ] JMH benchmarks for add, cancel and an aggressive match
- [ ] Full replay, timed per message into HdrHistogram (warm up first)
- [ ] Report p50 / p90 / p99 / p99.9 / max plus throughput
- [ ] CPU and allocation flame graphs with async-profiler
- [ ] Fill in the v1.0 row of the results log

**Gate:** v1.0 numbers are recorded, repeatable within noise across 3 runs, with the method written down.

---

### Phase 4: Optimize the hot path (weeks 7–9)
One change at a time, one results-log row each. Re-run the v1-vs-v2 property test after every change.
- [ ] Array-indexed price levels with a cached best price, plus the overflow slow path (with a counter)
- [ ] Intrusive doubly-linked lists inside levels
- [ ] Primitive id map
- [ ] Order object pool
- [ ] Flyweight decoder with no per-message objects
- [ ] Run under Epsilon GC with a small heap
- [ ] Compare G1 vs ZGC tails
- [ ] Re-capture flame graphs; `perf stat` on Linux for cache misses and branch mispredictions

**Gate:** Epsilon GC run survives, and the v1-vs-v2 property test is still green.

**→ Minimum shippable project. If time or energy is running low, jump to Phase 6 now.**

---

### Phase 5: Threaded pipeline (weeks 10–11) — stretch, needs Linux
- [ ] Write your own SPSC ring buffer (padded sequences, acquire/release ordering)
- [ ] Parser thread → ring buffer → matcher thread, both busy-spinning and pinned to cores
- [ ] Measure end to end
- [ ] Swap in the LMAX Disruptor and compare
- [ ] Add rows v3.0 and v3.1 to the results log

**Gate:** both pipelines produce identical event streams, and both rows are in the results log.

---

### Phase 6: Write up and ship (week 12)
- [ ] README, top to bottom:
  1. One-line summary with the headline number
  2. Architecture diagram
  3. Results log table
  4. One flame graph
  5. Benchmark method (machine, CPU model, flags, warmup, runs)
  6. How to run it
- [ ] Both diagrams (architecture, and book data structure)
- [ ] Blog post: "From X µs to Y ns: optimizing a Java order book", walking through 3 or 4 changes from the results log and why each helped

---

### Phase 7: C++ port — optional stretch
- Port the core book to C++ with the same design.
- Benchmark it with Google Benchmark, check it with ASan/UBSan, and profile it with `perf`.
- Story: "Same design, Java vs C++, here's where the nanoseconds went." Most HFT roles at Indian firms like Graviton and iRage are C++-heavy, so this strengthens the pitch.

---

## 12. Pitfalls

- Using `double` for prices. Rounding errors break equality at a price level. Use `long` ticks.
- Optimizing before the naive version is correct and tested.
- Benchmarking without warmup, or reporting averages instead of percentiles.
- Cancels that scan a list to find the order (O(n)). Always keep an id-to-order map.
- Leaving empty price levels in the book, which corrupts best-price tracking.
- Growing scope (multiple symbols, networking, a UI) before the core numbers exist.
- Trusting AI-generated benchmarks or hot-path code without reading every line.
- Publishing numbers measured on macOS as if they were tuned Linux numbers.

## 13. Debugging habits (from day one)

- **Deterministic replay:** log every input message. Any bug becomes "replay this file" and reproduces exactly.
- **Invariant checker:** one `checkInvariants()` method, run after every event in tests.
- **Shrink failures:** when a jqwik property fails, it shrinks the input to the smallest failing case. Read that case first.
- **Conditional breakpoints** in IntelliJ on a specific order id, so you stop exactly when that order is touched.
- **Golden-file tests** for ITCH: process a small slice, save the book as text, compare on every run.

---

## 14. Resources

Listed from memory in the original plan, so verify the titles before relying on them.

| Resource | Read during | What you get from it |
|---|---|---|
| WK Selph, "How to Build a Fast Limit Order Book" (blog post) | Week 1 | The classic short design note for this exact project |
| NASDAQ TotalView-ITCH 5.0 specification (PDF) | Weeks 1, 4 | Byte layouts of every message type |
| Martin Fowler, "The LMAX Architecture" | Week 1 | Why single-threaded matching beats locks |
| Larry Harris, *Trading and Exchanges* | Ongoing | Market microstructure; chapters on order types and matching |
| OpenJDK JMH samples (in the JMH repo) | Week 6 | How to write benchmarks that don't lie |
| Gil Tene, "How NOT to Measure Latency" (talk) | Week 6 | Coordinated omission and percentile reporting |
| async-profiler README | Week 6 | Flame graphs for CPU and allocations |
| Martin Thompson, Mechanical Sympathy blog | Weeks 7–10 | Caches, false sharing, ring buffers |
| LMAX Disruptor source code | Week 10 | A production ring buffer to compare against yours |
| Scott Oaks, *Java Performance* | Ongoing | GC, JIT and JVM-flag reference |

---

## 15. Turning it into a career signal

The repo gets you an interview; the write-up and your ability to defend every number get you the offer.

- **Outreach line:** "I built an order book that rebuilds a day of NASDAQ ITCH at p99 of Y ns with zero GC." That's a far better opener to an HFT engineer than a resume link.
- **Be ready to explain:**
  - why arrays beat trees here
  - what coordinated omission is
  - what happens when a price falls outside your band, and how often it fired
  - how you proved zero allocation
  - why matching is single-threaded
  - what you'd change in C++
  - why each results-log row moved the numbers it did
